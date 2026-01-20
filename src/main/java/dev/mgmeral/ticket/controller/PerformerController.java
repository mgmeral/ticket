package dev.mgmeral.ticket.controller;

import dev.mgmeral.ticket.model.PerformerCreateRequest;
import dev.mgmeral.ticket.model.PerformerResponse;
import dev.mgmeral.ticket.model.PerformerUpdateRequest;
import dev.mgmeral.ticket.service.PerformerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/performers")
public class PerformerController {


    private final PerformerService performerService;

    public PerformerController(PerformerService performerService) {
        this.performerService = performerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PerformerResponse create(@Valid @RequestBody PerformerCreateRequest request) {
        return performerService.create(request);
    }

    @GetMapping("/{id}")
    public PerformerResponse getById(@PathVariable Long id) {
        return performerService.getById(id);
    }

    @GetMapping
    public Page<PerformerResponse> list(@RequestParam(required = false) String name,
                                        Pageable pageable) {
        return performerService.list(name, pageable);
    }

    @PutMapping("/{id}")
    public PerformerResponse update(@PathVariable Long id,
                                    @Valid @RequestBody PerformerUpdateRequest request) {
        return performerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        performerService.delete(id);
    }
}
