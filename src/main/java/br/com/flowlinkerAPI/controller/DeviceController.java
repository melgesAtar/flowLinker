package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.config.security.CurrentRequest;
import br.com.flowlinkerAPI.dto.device.HeartbeatRequest;
import br.com.flowlinkerAPI.dto.device.HeartbeatResponse;
import br.com.flowlinkerAPI.model.AppRelease;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.model.DeviceStatus;
import br.com.flowlinkerAPI.repository.DeviceRepository;
import br.com.flowlinkerAPI.repository.AppReleaseRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final AppReleaseRepository appReleaseRepository;
    private final CurrentRequest currentRequest;

    public DeviceController(DeviceRepository deviceRepository, AppReleaseRepository appReleaseRepository, CurrentRequest currentRequest) {
        this.deviceRepository = deviceRepository;
        this.appReleaseRepository = appReleaseRepository;
        this.currentRequest = currentRequest;
    }

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
