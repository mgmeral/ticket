package dev.mgmeral.ticket.model;

import java.time.Instant;

public record SeanceCreateResponse(Long id,
                                   Long eventId,
                                   Instant startDateTime,
                                   int capacity
) {
}