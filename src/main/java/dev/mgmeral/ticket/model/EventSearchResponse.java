package dev.mgmeral.ticket.model;

import java.time.Instant;

public record EventSearchResponse(
        Long id,
        String type,
        String name,
        Instant startDate,
        Instant endDate
) {
}