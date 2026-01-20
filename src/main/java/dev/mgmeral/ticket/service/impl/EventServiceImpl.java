package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Event;
import dev.mgmeral.ticket.enums.EventType;
import dev.mgmeral.ticket.mapper.EventMapper;
import dev.mgmeral.ticket.model.*;
import dev.mgmeral.ticket.repository.EventRepository;
import dev.mgmeral.ticket.repository.PerformerRepository;
import dev.mgmeral.ticket.repository.spec.EventSpecifications;
import dev.mgmeral.ticket.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Transactional
@Service
@Slf4j
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final PerformerRepository performerRepository;

    public EventServiceImpl(EventRepository eventRepository, EventMapper eventMapper, PerformerRepository performerRepository) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.performerRepository = performerRepository;
    }

    private static String safe(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @Override
    public EventCreateResponse create(EventCreateRequest request) {
        Event event = eventMapper.toEntity(request);

        log.info("event.create.start name={} type={} startDate={} performerIdsCount={}",
                safe(request.name()),
                safe(request.type()),
                request.startDate(),
                request.performerIds() == null ? 0 : request.performerIds().size());

        applyPerformers(event, request.performerIds());
        event = eventRepository.save(event);

        log.info("event.create.ok eventId={} name={} type={} startDate={} performersCount={}",
                event.getId(),
                safe(event.getName()),
                event.getType(),
                event.getStartDate(),
                event.getPerformers() == null ? 0 : event.getPerformers().size());

        return eventMapper.toCreateResponse(event);
    }

    @Override
    public EventGetResponse getById(Long id) {
        log.debug("event.getById.start eventId={}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));

        log.debug("event.getById.ok eventId={} name={} type={}", event.getId(), safe(event.getName()), event.getType());
        return eventMapper.toGetResponse(event);
    }

    @Override
    public EventUpdateResponse update(Long id, EventUpdateRequest request) {
        log.info("event.update.start eventId={} name={} type={} startDate={} performerIdsProvided={}",
                id,
                safe(request.name()),
                safe(request.type()),
                request.startDate(),
                request.performerIds() != null);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));

        eventMapper.updateEntity(event, request);
        if (request.performerIds() != null) {
            applyPerformers(event, request.performerIds());
        }

        Event saved = eventRepository.save(event);

        log.info("event.update.ok eventId={} name={} type={} startDate={} performersCount={}",
                saved.getId(),
                safe(saved.getName()),
                saved.getType(),
                saved.getStartDate(),
                saved.getPerformers() == null ? 0 : saved.getPerformers().size());

        return eventMapper.toUpdateResponse(saved);
    }

    @Override
    public void delete(Long id) {
        log.info("event.delete.start eventId={}", id);

        if (!eventRepository.existsById(id)) {
            log.warn("event.delete.notFound eventId={}", id);
            throw new EntityNotFoundException("Event not found: " + id);
        }

        eventRepository.deleteById(id);
        log.info("event.delete.ok eventId={}", id);
    }

    @Override
    public Page<EventSearchResponse> search(String type, String name, Instant startFrom, Instant startTo, Pageable pageable) {
        log.debug("event.search params type={} name={} startFrom={} startTo={} page={} size={} sort={}",
                safe(type), safe(name), startFrom, startTo,
                pageable == null ? null : pageable.getPageNumber(),
                pageable == null ? null : pageable.getPageSize(),
                pageable == null ? null : pageable.getSort());

        Specification<Event> spec = Specification.where(null);

        if (type != null && !type.isBlank()) {
            EventType eventType = EventType.valueOf(type.trim().toUpperCase());
            spec = spec.and(EventSpecifications.typeEquals(eventType));
        }
        if (name != null && !name.isBlank()) {
            spec = spec.and(EventSpecifications.nameContains(name.trim()));
        }
        if (startFrom != null) {
            spec = spec.and(EventSpecifications.startDateGte(startFrom));
        }
        if (startTo != null) {
            spec = spec.and(EventSpecifications.startDateLte(startTo));
        }

        Page<EventSearchResponse> page = eventRepository.findAll(spec, pageable)
                .map(eventMapper::toSearchResponse);

        log.debug("event.search.result totalElements={} totalPages={} pageNumber={}",
                page.getTotalElements(), page.getTotalPages(), page.getNumber());

        return page;
    }

    @Override
    public EventUpdateResponse updatePerformers(Long eventId, EventPerformerUpdateRequest request) {
        log.info("event.updatePerformers.start eventId={} performerIdsCount={}",
                eventId, request.performerIds() == null ? 0 : request.performerIds().size());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));

        applyPerformers(event, request.performerIds());
        Event saved = eventRepository.save(event);

        log.info("event.updatePerformers.ok eventId={} performersCount={}",
                saved.getId(), saved.getPerformers() == null ? 0 : saved.getPerformers().size());

        return eventMapper.toUpdateResponse(saved);
    }

    private void applyPerformers(Event event, Set<Long> performerIds) {
        int requested = performerIds == null ? 0 : performerIds.size();
        log.debug("event.applyPerformers.start eventId={} performerIdsCount={}", event.getId(), requested);

        if (performerIds == null || performerIds.isEmpty()) {
            event.getPerformers().clear();
            log.debug("event.applyPerformers.cleared eventId={}", event.getId());
            return;
        }

        var performers = performerRepository.findAllById(performerIds);
        if (performers.size() != performerIds.size()) {
            Set<Long> found = new HashSet<>();
            for (var p : performers) found.add(p.getId());

            Set<Long> missing = new HashSet<>(performerIds);
            missing.removeAll(found);

            log.warn("event.applyPerformers.missing eventId={} missingPerformerIds={}", event.getId(), missing);
            throw new EntityNotFoundException("Performer(s) not found: " + missing);
        }

        event.getPerformers().clear();
        event.getPerformers().addAll(performers);

        log.debug("event.applyPerformers.ok eventId={} performersCount={}",
                event.getId(), event.getPerformers().size());
    }
}
