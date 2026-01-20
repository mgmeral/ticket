package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Performer;
import dev.mgmeral.ticket.model.PerformerCreateRequest;
import dev.mgmeral.ticket.model.PerformerResponse;
import dev.mgmeral.ticket.model.PerformerUpdateRequest;
import dev.mgmeral.ticket.repository.PerformerRepository;
import dev.mgmeral.ticket.service.PerformerService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class PerformerServiceImpl implements PerformerService {
    private final PerformerRepository performerRepository;

    public PerformerServiceImpl(PerformerRepository performerRepository) {
        this.performerRepository = performerRepository;
    }

    private static String safe(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @Override
    public PerformerResponse create(PerformerCreateRequest request) {
        String name = request.name();

        log.info("performer.create.start name={} role={}", safe(name), safe(request.role()));

        if (performerRepository.existsByNameIgnoreCase(name)) {
            log.warn("performer.create.duplicate name={}", safe(name));
            throw new IllegalArgumentException("Performer already exists: " + name);
        }

        Performer performer = new Performer();
        performer.setName(name);
        performer.setRole(request.role());
        performer.setDescription(request.description());

        Performer saved = performerRepository.save(performer);

        log.info("performer.create.ok performerId={} name={} role={}",
                saved.getId(), safe(saved.getName()), safe(saved.getRole()));

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PerformerResponse getById(Long id) {
        log.debug("performer.getById.start performerId={}", id);

        Performer performer = performerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Performer not found: " + id));

        log.debug("performer.getById.ok performerId={} name={}", performer.getId(), safe(performer.getName()));
        return toResponse(performer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PerformerResponse> list(String name, Pageable pageable) {
        log.debug("performer.list params name={} page={} size={} sort={}",
                safe(name),
                pageable == null ? null : pageable.getPageNumber(),
                pageable == null ? null : pageable.getPageSize(),
                pageable == null ? null : pageable.getSort());

        Page<PerformerResponse> page;
        if (name == null || name.isBlank()) {
            page = performerRepository.findAll(pageable).map(this::toResponse);
        } else {
            page = performerRepository.findByNameContainingIgnoreCase(name.trim(), pageable).map(this::toResponse);
        }

        log.debug("performer.list.result totalElements={} totalPages={} pageNumber={}",
                page.getTotalElements(), page.getTotalPages(), page.getNumber());

        return page;
    }

    @Override
    public PerformerResponse update(Long id, PerformerUpdateRequest request) {
        log.info("performer.update.start performerId={} name={} role={}",
                id, safe(request.name()), safe(request.role()));

        Performer performer = performerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Performer not found: " + id));

        performer.setName(request.name());
        performer.setRole(request.role());
        performer.setDescription(request.description());

        Performer saved = performerRepository.save(performer);

        log.info("performer.update.ok performerId={} name={} role={}",
                saved.getId(), safe(saved.getName()), safe(saved.getRole()));

        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        log.info("performer.delete.start performerId={}", id);

        if (!performerRepository.existsById(id)) {
            log.warn("performer.delete.notFound performerId={}", id);
            throw new EntityNotFoundException("Performer not found: " + id);
        }

        performerRepository.deleteById(id);
        log.info("performer.delete.ok performerId={}", id);
    }

    private PerformerResponse toResponse(Performer p) {
        return new PerformerResponse(p.getId(), p.getName(), p.getRole(), p.getDescription());
    }
}
