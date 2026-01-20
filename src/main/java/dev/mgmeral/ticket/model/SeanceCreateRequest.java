package dev.mgmeral.ticket.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SeanceCreateRequest(
        @NotNull Instant startDateTime,
        @Min(1) int capacity
) {
}