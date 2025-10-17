package br.com.flowlinkerAPI.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Setter;

import java.time.Instant;

@Entity
@Setter
public class StripeProcessedEvent {
    @Id
    private String eventId;
    private String eventType;
    private Instant processedAt;
}
