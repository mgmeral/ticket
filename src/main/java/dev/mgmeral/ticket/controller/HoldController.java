package dev.mgmeral.ticket.controller;

import dev.mgmeral.ticket.model.HoldCreateRequest;
import dev.mgmeral.ticket.model.HoldResponse;
import dev.mgmeral.ticket.service.HoldService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/holds")
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse create(@Valid @RequestBody HoldCreateRequest request) {
        return holdService.create(request);
    }

    @GetMapping("/{holdId}")
    public HoldResponse getById(@PathVariable Long holdId) {
        return holdService.getById(holdId);
    }

    @DeleteMapping("/{holdId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long holdId) {
        holdService.release(holdId);
    }
}
