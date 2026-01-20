package dev.mgmeral.ticket.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentAuthorizeRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
}
