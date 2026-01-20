package dev.mgmeral.ticket.controller;

import dev.mgmeral.ticket.model.*;
import dev.mgmeral.ticket.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventCreateResponse create(@Valid @RequestBody EventCreateRequest request) {
        return eventService.create(request);
    }

    @GetMapping("/{id}")
    public EventGetResponse getById(@PathVariable @Min(1) Long id) {
        return eventService.getById(id);
    }

    @PutMapping("/{id}")
    public EventUpdateResponse update(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Min(1) Long id) {
        eventService.delete(id);
    }

    @GetMapping
    public Page<EventSearchResponse> search(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return eventService.search(type, name, startFrom, startTo, pageable);
    }

    @PutMapping("/{id}/performers")
    public EventUpdateResponse updatePerformers(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody EventPerformerUpdateRequest request
    ) {
        return eventService.updatePerformers(id, request);
    }
}
