package dev.mgmeral.ticket.model;

import dev.mgmeral.ticket.enums.PurchaseStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseResponse(
        Long purchaseId,
        Long holdId,
        Long userId,
        Long seanceId,
        int quantity,
        BigDecimal amount,
        String paymentRef,
        PurchaseStatus status,
        Instant createdAt
) {
}
