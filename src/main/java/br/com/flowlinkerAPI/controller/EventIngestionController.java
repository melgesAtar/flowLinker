// src/main/java/br/com/flowlinkerAPI/controller/EventIngestionController.java
package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.config.security.CurrentRequest;
import br.com.flowlinkerAPI.dto.event.EnrichedEventDTO;
import br.com.flowlinkerAPI.dto.event.IngestEventDTO;
import br.com.flowlinkerAPI.dto.event.IngestEventsRequest;
import br.com.flowlinkerAPI.model.Device;
import br.com.flowlinkerAPI.service.EventPublisherService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ingest")
@Validated
public class EventIngestionController {

	private final EventPublisherService publisher;
	private final CurrentRequest currentRequest;
	private static final Logger logger = LoggerFactory.getLogger(EventIngestionController.class);

	public EventIngestionController(EventPublisherService publisher, CurrentRequest currentRequest) {
		this.publisher = publisher;
		this.currentRequest = currentRequest;
	}

	@PostMapping("/events")
	public ResponseEntity<?> ingest(@Valid @RequestBody IngestEventsRequest body, HttpServletRequest req) {
		var cu = currentRequest.get();
		if (cu == null) {
			return ResponseEntity.status(401).body(Map.of("code","UNAUTHENTICATED","message","Autenticação requerida"));
		}

		Device device = currentRequest.getDevice(); 
		Long customerId = currentRequest.getCustomerId();

		String ip = extractIp(req);

		List<IngestEventDTO> events = body.events();
		Instant receivedAt = Instant.now();

		for (IngestEventDTO e : events) {
			if (e == null) {
				continue;
			}
			// Loga apenas o tipo do evento recebido
			if (e.eventType() != null) {
				logger.info("ingest eventType={}", e.eventType());
			}
			String eventId = (e.eventId() != null && !e.eventId().isBlank()) ? e.eventId() : UUID.randomUUID().toString();

			EnrichedEventDTO enriched = new EnrichedEventDTO(
					eventId,
					e.eventType(),
					e.eventAt(),
					receivedAt,
					e.payload(),
					customerId,
					device != null ? device.getId() : null,
					ip
			);

			publisher.publish(enriched);
		}

		return ResponseEntity.accepted().body(Map.of(
				"accepted", events.size(),
				"receivedAt", receivedAt.toString()
		));
	}

	private String extractIp(HttpServletRequest req) {
		String xf = req.getHeader("X-Forwarded-For");
		if (xf != null && !xf.isBlank()) {
			int comma = xf.indexOf(',');
			return comma > 0 ? xf.substring(0, comma).trim() : xf.trim();
		}
		return req.getRemoteAddr();
	}
}