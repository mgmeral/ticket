package dev.mgmeral.ticket.service;

import dev.mgmeral.ticket.model.PurchaseCreateRequest;
import dev.mgmeral.ticket.model.PurchaseCreateResult;

public interface PurchaseService {
    PurchaseCreateResult create(PurchaseCreateRequest request);
}
