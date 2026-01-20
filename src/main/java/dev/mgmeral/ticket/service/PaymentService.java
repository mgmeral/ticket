package dev.mgmeral.ticket.service;

import dev.mgmeral.ticket.model.PaymentAuthorizeRequest;
import dev.mgmeral.ticket.model.PaymentAuthorizeResponse;

public interface PaymentService {
    PaymentAuthorizeResponse authorize(PaymentAuthorizeRequest request);

}
