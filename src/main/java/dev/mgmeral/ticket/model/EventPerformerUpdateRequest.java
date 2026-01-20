package dev.mgmeral.ticket.model;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record EventPerformerUpdateRequest(
        @NotEmpty Set<Long> performerIds
) {
}
