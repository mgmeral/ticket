package dev.mgmeral.ticket.service;

import dev.mgmeral.ticket.model.PerformerCreateRequest;
import dev.mgmeral.ticket.model.PerformerResponse;
import dev.mgmeral.ticket.model.PerformerUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PerformerService {

    PerformerResponse create(PerformerCreateRequest request);

    PerformerResponse getById(Long id);

    Page<PerformerResponse> list(String name, Pageable pageable);

    PerformerResponse update(Long id, PerformerUpdateRequest request);

    void delete(Long id);
}
