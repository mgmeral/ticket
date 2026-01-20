package dev.mgmeral.ticket.service;

import dev.mgmeral.ticket.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface EventService {

    EventCreateResponse create(EventCreateRequest request);

    EventGetResponse getById(Long id);

    EventUpdateResponse update(Long id, EventUpdateRequest request);

    void delete(Long id);

    Page<EventSearchResponse> search(String type, String name, Instant startFrom, Instant startTo, Pageable pageable);

    EventUpdateResponse updatePerformers(Long eventId, EventPerformerUpdateRequest request);
}
