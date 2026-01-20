package dev.mgmeral.ticket.controller;

import dev.mgmeral.ticket.model.PurchaseCreateRequest;
import dev.mgmeral.ticket.model.PurchaseResponse;
import dev.mgmeral.ticket.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseCreateRequest request) {
        var result = purchaseService.create(request);
        return result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(result.response())
                : ResponseEntity.ok(result.response());
    }
}
