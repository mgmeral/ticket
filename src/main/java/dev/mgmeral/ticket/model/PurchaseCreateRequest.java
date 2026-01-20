package dev.mgmeral.ticket.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PurchaseCreateRequest(
        @NotNull Long holdId,
        @NotBlank @Size(max = 64) String paymentRef,
        @NotBlank @Size(max = 80) String idempotencyKey
) {
}
