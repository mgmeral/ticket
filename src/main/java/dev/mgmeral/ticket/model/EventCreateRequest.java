package dev.mgmeral.ticket.model;

import dev.mgmeral.ticket.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record EventCreateRequest(
        @NotNull EventType type,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String summary,
        @Size(max = 2000) String description,
        Instant startDate,
        Instant endDate,
        Set<Long> performerIds
) {
}
