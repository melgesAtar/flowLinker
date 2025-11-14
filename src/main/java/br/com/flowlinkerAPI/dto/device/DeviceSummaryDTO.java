package br.com.flowlinkerAPI.dto.device;

import br.com.flowlinkerAPI.model.DeviceStatus;
import java.time.Instant;

public record DeviceSummaryDTO(
        Long id,
        String name,
        String fingerprint,
        String deviceId,
        DeviceStatus status,
        String osName,
        String osVersion,
        String appVersion,
        String lastIp,
        Instant lastSeenAt,
        Instant createdAt
) {}

