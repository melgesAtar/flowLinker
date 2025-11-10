// src/main/java/br/com/flowlinkerAPI/dto/event/EnrichedEventDTO.java
package br.com.flowlinkerAPI.dto.event;

import java.time.Instant;
import java.util.Map;

public record EnrichedEventDTO(
	String eventId,
	String eventType,
	Instant eventAt,
	Instant receivedAt,
	Map<String, Object> payload,
	Long customerId,
	Long deviceId,
	String ip
) {}