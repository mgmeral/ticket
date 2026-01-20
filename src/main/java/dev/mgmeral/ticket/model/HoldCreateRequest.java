package dev.mgmeral.ticket.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HoldCreateRequest(
        @NotNull Long userId,
        @NotNull Long seanceId,
        @Min(1) int quantity,
        @NotBlank @Size(max = 80) String idempotencyKey
) {
}
