package dev.mgmeral.ticket.model;

import dev.mgmeral.ticket.enums.HoldStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record HoldResponse(
        Long id,
        Long userId,
        Long seanceId,
        int quantity,
        HoldStatus status,
        Instant expiresAt,
        BigDecimal totalPrice
) {
}
