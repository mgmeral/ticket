package dev.mgmeral.ticket.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerformerCreateRequest(
        @NotBlank @Size(min = 3, max = 200) String name,
        @NotBlank @Size(min = 2, max = 100) String role,
        @Size(max = 2000) String description) {
}
