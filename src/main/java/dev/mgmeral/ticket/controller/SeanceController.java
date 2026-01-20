package dev.mgmeral.ticket.controller;

import dev.mgmeral.ticket.model.SeanceAvailabilityResponse;
import dev.mgmeral.ticket.model.SeanceCreateRequest;
import dev.mgmeral.ticket.model.SeanceCreateResponse;
import dev.mgmeral.ticket.model.SeanceGetResponse;
import dev.mgmeral.ticket.service.SeanceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
public class SeanceController {

    private final SeanceService seanceService;

    public SeanceController(SeanceService seanceService) {
        this.seanceService = seanceService;
    }

    @PostMapping("/events/{eventId}/seances")
    @ResponseStatus(HttpStatus.CREATED)
    public SeanceCreateResponse create(@PathVariable Long eventId, @Valid @RequestBody SeanceCreateRequest request) {
        return seanceService.create(eventId, request);
    }

    @GetMapping("/seances/{id}")
    public SeanceGetResponse getById(@PathVariable Long id) {
        return seanceService.getById(id);
    }

    @GetMapping("/seances")
    public Page<SeanceGetResponse> search(
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return seanceService.search(eventId, dateFrom, dateTo, pageable);
    }

    @GetMapping("/seances/{id}/availability")
    public SeanceAvailabilityResponse availability(@PathVariable Long id) {
        return seanceService.availability(id);
    }
}
