package dev.mgmeral.ticket.service;

import dev.mgmeral.ticket.model.SeanceAvailabilityResponse;
import dev.mgmeral.ticket.model.SeanceCreateRequest;
import dev.mgmeral.ticket.model.SeanceCreateResponse;
import dev.mgmeral.ticket.model.SeanceGetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface SeanceService {
    SeanceCreateResponse create(Long eventId, SeanceCreateRequest request);

    SeanceGetResponse getById(Long id);

    Page<SeanceGetResponse> search(Long eventId, Instant dateFrom, Instant dateTo, Pageable pageable);

    SeanceAvailabilityResponse availability(Long id);
}
