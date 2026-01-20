package dev.mgmeral.ticket.service;

import dev.mgmeral.ticket.entity.Hold;
import dev.mgmeral.ticket.model.HoldCreateRequest;
import dev.mgmeral.ticket.model.HoldResponse;

import java.time.Instant;

public interface HoldService {
    HoldResponse create(HoldCreateRequest request);

    HoldResponse getById(Long holdId);

    void release(Long holdId);

    boolean expireIfNeeded(Hold hold, Instant now);
}
