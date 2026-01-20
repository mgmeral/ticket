package dev.mgmeral.ticket.model;

public record SeanceAvailabilityResponse(
        Long seanceId,
        int capacity,
        long soldCount,
        long heldCount,
        long availableCount
) {
}