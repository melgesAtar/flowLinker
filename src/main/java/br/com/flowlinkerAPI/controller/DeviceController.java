package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.config.security.CurrentRequest;
import br.com.flowlinkerAPI.dto.device.HeartbeatRequest;
import br.com.flowlinkerAPI.dto.device.HeartbeatResponse;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.model.DeviceStatus;
import br.com.flowlinkerAPI.repository.DeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/devices")
public class DeviceController {

    private final DeviceRepository deviceRepository;
    private final CurrentRequest currentRequest;

    public DeviceController(DeviceRepository deviceRepository, CurrentRequest currentRequest) {
        this.deviceRepository = deviceRepository;
        this.currentRequest = currentRequest;
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

        HeartbeatResponse resp = new HeartbeatResponse();
        resp.deviceId = device.getDeviceId();
        resp.status = device.getStatus() != null ? device.getStatus().name() : null;
        resp.serverTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        return ResponseEntity.ok(resp);
    }
}
