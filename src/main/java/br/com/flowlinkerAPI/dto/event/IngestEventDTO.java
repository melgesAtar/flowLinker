// src/main/java/br/com/flowlinkerAPI/dto/event/IngestEventDTO.java
package br.com.flowlinkerAPI.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

public record IngestEventDTO(
	String eventId,                
	@NotBlank String eventType,     
	@NotNull Instant eventAt,              
	Map<String, Object> payload                       
) {}