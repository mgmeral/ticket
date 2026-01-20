package dev.mgmeral.ticket.model;

import dev.mgmeral.ticket.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentAuthorizeResponse(
        String paymentRef,
        PaymentStatus status,
        BigDecimal amount,
        Instant createdAt
) {
}