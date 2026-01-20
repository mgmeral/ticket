package dev.mgmeral.ticket.controller;

import dev.mgmeral.ticket.model.PaymentAuthorizeRequest;
import dev.mgmeral.ticket.model.PaymentAuthorizeResponse;
import dev.mgmeral.ticket.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    @ResponseStatus(HttpStatus.OK)
    public PaymentAuthorizeResponse authorize(@Valid @RequestBody PaymentAuthorizeRequest request) {
        return paymentService.authorize(request);
    }
}
