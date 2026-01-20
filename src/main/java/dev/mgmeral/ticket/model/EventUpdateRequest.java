package dev.mgmeral.ticket.model;

import dev.mgmeral.ticket.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record EventUpdateRequest(
        @NotNull @Size(max = 30) EventType type,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String summary,
        Instant startDate,
        Instant endDate,
        Set<Long> performerIds) {
}
