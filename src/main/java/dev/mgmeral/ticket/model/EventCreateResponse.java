package dev.mgmeral.ticket.model;

import dev.mgmeral.ticket.enums.EventType;

import java.time.Instant;

public record EventCreateResponse(
        Long id,
        EventType type,
        String name,
        String description,
        String summary,
        Instant startDate,
        Instant endDate
) {
}