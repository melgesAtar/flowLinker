// src/main/java/br/com/flowlinkerAPI/service/EventPublisherService.java
package br.com.flowlinkerAPI.service;

import br.com.flowlinkerAPI.config.EventIngestionRabbitConfig;
import br.com.flowlinkerAPI.dto.event.EnrichedEventDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

	private final RabbitTemplate rabbitTemplate;

	public EventPublisherService(RabbitTemplate eventsRabbitTemplate) {
		this.rabbitTemplate = eventsRabbitTemplate;
	}

	public void publish(EnrichedEventDTO e) {
		String routingKey = normalizeRoutingKey(e.eventType());
		rabbitTemplate.convertAndSend(
				EventIngestionRabbitConfig.EXCHANGE_EVENTS,
				routingKey,
				e,
				msg -> {
					msg.getMessageProperties().setHeader("eventId", e.eventId());
					msg.getMessageProperties().setHeader("customerId", e.customerId());
					msg.getMessageProperties().setHeader("deviceId", e.deviceId());
					msg.getMessageProperties().setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
					return msg;
				}
		);
	}

	private String normalizeRoutingKey(String eventType) {

		return eventType.toLowerCase().replace(' ', '.');
	}
}