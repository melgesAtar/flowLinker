// src/main/java/br/com/flowlinkerAPI/dto/event/IngestEventsRequest.java
package br.com.flowlinkerAPI.dto.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record IngestEventsRequest(@NotEmpty List<@Valid IngestEventDTO> events) {}