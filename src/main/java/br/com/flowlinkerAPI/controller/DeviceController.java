package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.config.security.CurrentRequest;
import br.com.flowlinkerAPI.dto.device.HeartbeatRequest;
import br.com.flowlinkerAPI.dto.device.HeartbeatResponse;
import br.com.flowlinkerAPI.model.AppRelease;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.model.DeviceStatus;
import br.com.flowlinkerAPI.dto.device.DeviceCountsDTO;
import br.com.flowlinkerAPI.dto.device.DeviceSummaryDTO;
import br.com.flowlinkerAPI.dto.device.UpdateDeviceStatusRequest;
import br.com.flowlinkerAPI.repository.DeviceRepository;
import br.com.flowlinkerAPI.repository.AppReleaseRepository;
import br.com.flowlinkerAPI.repository.CustomerRepository;
import br.com.flowlinkerAPI.service.DevicePolicyService;
import br.com.flowlinkerAPI.exceptions.LimitDevicesException;
import br.com.flowlinkerAPI.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;

@RestController
@RequestMapping("/devices")
@Tag(name = "Devices", description = "Gerenciamento de dispositivos do cliente e heartbeat do app desktop")
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final AppReleaseRepository appReleaseRepository;
    private final CurrentRequest currentRequest;
    private final CustomerRepository customerRepository;
    private final DevicePolicyService devicePolicyService;
    private final DeviceService deviceService;

    public DeviceController(DeviceRepository deviceRepository,
                            AppReleaseRepository appReleaseRepository,
                            CurrentRequest currentRequest,
                            CustomerRepository customerRepository,
                            DevicePolicyService devicePolicyService,
                            DeviceService deviceService) {
        this.deviceRepository = deviceRepository;
        this.appReleaseRepository = appReleaseRepository;
        this.currentRequest = currentRequest;
        this.customerRepository = customerRepository;
        this.devicePolicyService = devicePolicyService;
        this.deviceService = deviceService;
    }

    @Operation(summary = "Heartbeat (GET)", description = "Endpoint legado de heartbeat para dispositivos.")
    @org.springframework.web.bind.annotation.GetMapping("/heartbeat")
    public org.springframework.http.ResponseEntity<HeartbeatResponse> heartbeatGet(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String deviceId,
            @org.springframework.web.bind.annotation.RequestParam String appVersion,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        Device device = currentRequest.getDevice();
        if (device == null) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
        if (deviceId != null && (device.getDeviceId() == null || device.getDeviceId().isBlank())) {
            device.setDeviceId(deviceId);
        }
        if (appVersion != null && !appVersion.isBlank()) {
            device.setAppVersion(appVersion);
        }
        device.setLastIp(servletRequest.getRemoteAddr());
        device.setLastSeenAt(java.time.Instant.now());
        deviceRepository.save(device);

        HeartbeatResponse resp = new HeartbeatResponse();
        resp.deviceValid = true;

        AppRelease.Platform platform = AppRelease.Platform.WIN; // foco atual: Windows MSI
        AppRelease.Arch arch = mapArch(device.getArch());
        var releaseOpt = appReleaseRepository.findFirstByPlatformAndArchAndIsActiveOrderByCreatedAtDesc(platform, arch, true);
        if (releaseOpt.isPresent()) {
            AppRelease r = releaseOpt.get();
            resp.latestVersion = r.getVersion();
            resp.minSupportedVersion = r.getMinimumVersion();
            resp.downloadUrl = r.getDownloadUrl();
            resp.sha256 = r.getSha256();
            resp.changelog = r.getReleaseNotes();
            boolean needsMin = r.getMinimumVersion() != null && isOlder(appVersion, r.getMinimumVersion());
            boolean needsForce = Boolean.TRUE.equals(r.getForceUpdate()) && isOlder(appVersion, r.getVersion());
            resp.mandatory = needsMin || needsForce;
        } else {
            resp.latestVersion = appVersion;
            resp.minSupportedVersion = null;
            resp.downloadUrl = null;
            resp.sha256 = null;
            resp.changelog = null;
            resp.mandatory = false;
        }

        return org.springframework.http.ResponseEntity.ok(resp);
    }

    @Operation(summary = "Listar dispositivos do cliente", description = "Retorna todos os dispositivos vinculados ao cliente autenticado. Use status=ACTIVE/INACTIVE para filtrar.")
    @org.springframework.web.bind.annotation.GetMapping("/mine")
    public org.springframework.http.ResponseEntity<?> myDevices(@org.springframework.web.bind.annotation.RequestParam(required = false) String status) {
        Long customerId = currentRequest.getCustomerId();
        if (customerId == null) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }
        DeviceStatus filter = null;
        if (status != null && !status.isBlank()) {
            try {
                filter = DeviceStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return org.springframework.http.ResponseEntity.badRequest()
                        .body(java.util.Map.of("code", "INVALID_STATUS"));
            }
        }
        java.util.List<DeviceSummaryDTO> devices = deviceService.listDevices(customerId, filter);
        return org.springframework.http.ResponseEntity.ok(devices);
    }

    @Operation(summary = "Contagem de dispositivos do cliente", description = "Retorna total, ativos, inativos e limite permitido para o cliente autenticado.")
    @org.springframework.web.bind.annotation.GetMapping("/mine/counts")
    public org.springframework.http.ResponseEntity<DeviceCountsDTO> myDeviceCounts() {
        Long customerId = currentRequest.getCustomerId();
        if (customerId == null) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }
        return org.springframework.http.ResponseEntity.ok(deviceService.getCounts(customerId));
    }

    @Operation(summary = "Atualizar status de um dispositivo", description = "Define o status (ACTIVE, INACTIVE etc.) de um device pertencente ao cliente autenticado.")
    @org.springframework.web.bind.annotation.PatchMapping("/{deviceId}/status")
    public org.springframework.http.ResponseEntity<?> updateStatus(@org.springframework.web.bind.annotation.PathVariable Long deviceId,
                                                                   @org.springframework.web.bind.annotation.RequestBody UpdateDeviceStatusRequest body) {
        Long customerId = currentRequest.getCustomerId();
        if (customerId == null) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }
        if (body == null || body.status() == null || body.status().isBlank()) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("code", "STATUS_REQUIRED"));
        }
        DeviceStatus status;
        try {
            status = DeviceStatus.valueOf(body.status().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("code", "INVALID_STATUS"));
        }
        DeviceSummaryDTO updated = deviceService.updateStatus(customerId, deviceId, status);
        return org.springframework.http.ResponseEntity.ok(updated);
    }

    @Operation(summary = "Limites de dispositivos por customerId", description = "Consulta limites e quantidade ativa para o cliente informado.")
    @org.springframework.web.bind.annotation.GetMapping("/limits")
    public org.springframework.http.ResponseEntity<?> getLimits(@Parameter(description = "ID do cliente") @org.springframework.web.bind.annotation.RequestParam Long customerId) {
        customerId = Objects.requireNonNull(customerId, "customerId");
        var customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return org.springframework.http.ResponseEntity.status(404).body(java.util.Map.of("code","CUSTOMER_NOT_FOUND"));
        }
        int active = deviceRepository.countByCustomerIdAndStatus(customerId, DeviceStatus.ACTIVE);
        int allowed = devicePolicyService.getAllowedDevices(customerId, customer.getOfferType());
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "customerId", customerId,
                "activeDevices", active,
                "allowedDevices", allowed,
                "offerTypeFallback", customer.getOfferType() != null ? customer.getOfferType().name() : null
        ));
    }

    private AppRelease.Arch mapArch(String arch) {
        if (arch == null) return AppRelease.Arch.X64;
        String a = arch.trim().toLowerCase();
        if ("arm64".equals(a) || "aarch64".equals(a)) return AppRelease.Arch.ARM64;
        return AppRelease.Arch.X64;
    }

    private boolean isOlder(String current, String target) {
        if (current == null || target == null) return false;
        String[] c = current.split("\\.");
        String[] t = target.split("\\.");
        int n = Math.max(c.length, t.length);
        for (int i = 0; i < n; i++) {
            int ci = i < c.length ? parseIntSafe(c[i]) : 0;
            int ti = i < t.length ? parseIntSafe(t[i]) : 0;
            if (ci < ti) return true;
            if (ci > ti) return false;
        }
        return false;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9].*$", "")); } catch (Exception e) { return 0; }
    }

    @Operation(summary = "Heartbeat (POST)", description = "Principal endpoint de heartbeat utilizado pelo app desktop.")
    @PostMapping("/heartbeat")
    public ResponseEntity<HeartbeatResponse> heartbeat(@RequestBody(required = false) HeartbeatRequest body,
                                                       HttpServletRequest servletRequest) {
        Device device = currentRequest.getDevice();
        if (device == null) {
            return ResponseEntity.badRequest().build();
        }
        String ip = (body != null && body.ip != null && !body.ip.isBlank())
                ? body.ip
                : servletRequest.getRemoteAddr();
        if (body != null && body.appVersion != null) {
            device.setAppVersion(body.appVersion);
        }
        if (body != null && body.status != null) {
            try {
                DeviceStatus s = DeviceStatus.valueOf(body.status.toUpperCase());
                // Bloqueia reativação caso o limite de devices ativos já tenha sido atingido
                if (s == DeviceStatus.ACTIVE && device.getStatus() != DeviceStatus.ACTIVE) {
                    if (device.getCustomer() != null) {
                        Long customerId = device.getCustomer().getId();
                        if (customerId == null) {
                            throw new IllegalArgumentException("Customer id is required");
                        }
                        int active = deviceRepository.countByCustomerIdAndStatus(customerId, DeviceStatus.ACTIVE);
                        int max = devicePolicyService.getAllowedDevices(customerId, device.getCustomer().getOfferType());
                        if (active >= max) {
                            throw new LimitDevicesException("Sem máquinas disponíveis. Revogue o acesso de uma máquina no painel administrativo para liberar uma vaga.");
                        }
                    }
                }
                device.setStatus(s);
            } catch (IllegalArgumentException ignored) { /* ignora status inválido */ }
        }
        device.setLastIp(ip);
        device.setLastSeenAt(Instant.now());
        deviceRepository.save(device);

        String currentVersion = (body != null && body.appVersion != null) ? body.appVersion : device.getAppVersion();
        HeartbeatResponse resp = new HeartbeatResponse();
        resp.deviceValid = true;
        AppRelease.Platform platform = AppRelease.Platform.WIN;
        AppRelease.Arch arch = mapArch(device.getArch());
        var releaseOpt = appReleaseRepository.findFirstByPlatformAndArchAndIsActiveOrderByCreatedAtDesc(platform, arch, true);
        if (releaseOpt.isPresent()) {
            AppRelease r = releaseOpt.get();
            resp.latestVersion = r.getVersion();
            resp.minSupportedVersion = r.getMinimumVersion();
            resp.downloadUrl = r.getDownloadUrl();
            resp.sha256 = r.getSha256();
            resp.changelog = r.getReleaseNotes();
            boolean needsMin = r.getMinimumVersion() != null && isOlder(currentVersion, r.getMinimumVersion());
            boolean needsForce = Boolean.TRUE.equals(r.getForceUpdate()) && isOlder(currentVersion, r.getVersion());
            resp.mandatory = needsMin || needsForce;
        } else {
            resp.latestVersion = currentVersion;
            resp.minSupportedVersion = null;
            resp.downloadUrl = null;
            resp.sha256 = null;
            resp.changelog = null;
            resp.mandatory = false;
        }
        return ResponseEntity.ok(resp);
    }
}
