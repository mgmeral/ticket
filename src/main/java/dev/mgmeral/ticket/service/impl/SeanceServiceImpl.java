package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Seance;
import dev.mgmeral.ticket.enums.HoldStatus;
import dev.mgmeral.ticket.enums.PurchaseStatus;
import dev.mgmeral.ticket.mapper.SeanceMapper;
import dev.mgmeral.ticket.model.SeanceAvailabilityResponse;
import dev.mgmeral.ticket.model.SeanceCreateRequest;
import dev.mgmeral.ticket.model.SeanceCreateResponse;
import dev.mgmeral.ticket.model.SeanceGetResponse;
import dev.mgmeral.ticket.repository.EventRepository;
import dev.mgmeral.ticket.repository.HoldRepository;
import dev.mgmeral.ticket.repository.PurchaseRepository;
import dev.mgmeral.ticket.repository.SeanceRepository;
import dev.mgmeral.ticket.service.SeanceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
@Slf4j
public class SeanceServiceImpl implements SeanceService {

    private final SeanceRepository seanceRepository;
    private final EventRepository eventRepository;
    private final HoldRepository holdRepository;
    private final PurchaseRepository purchaseRepository;
    private final SeanceMapper seanceMapper;

    public SeanceServiceImpl(SeanceRepository seanceRepository,
                             EventRepository eventRepository,
                             HoldRepository holdRepository,
                             PurchaseRepository purchaseRepository,
                             SeanceMapper seanceMapper) {
        this.seanceRepository = seanceRepository;
        this.eventRepository = eventRepository;
        this.holdRepository = holdRepository;
        this.purchaseRepository = purchaseRepository;
        this.seanceMapper = seanceMapper;
    }

    @Override
    public SeanceCreateResponse create(Long eventId, SeanceCreateRequest request) {
        log.info("seance.create.start eventId={} startDate={} capacity={}",
                eventId, request.startDateTime(), request.capacity());

        if (!eventRepository.existsById(eventId)) {
            log.warn("seance.create.eventNotFound eventId={}", eventId);
            throw new EntityNotFoundException("Event not found: " + eventId);
        }

        Seance seance = seanceMapper.toEntity(eventId, request);
        Seance saved = seanceRepository.save(seance);

        log.info("seance.create.ok seanceId={} eventId={} startDate={} capacity={}",
                saved.getId(), eventId, saved.getStartDate(), saved.getCapacity());

        return seanceMapper.toCreateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SeanceGetResponse getById(Long id) {
        log.debug("seance.getById.start seanceId={}", id);

        Seance seance = seanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Seance not found: " + id));

        log.debug("seance.getById.ok seanceId={} eventId={} startDate={} capacity={}",
                seance.getId(),
                seance.getEvent() != null ? seance.getEvent().getId() : null,
                seance.getStartDate(),
                seance.getCapacity());

        return seanceMapper.toGetResponse(seance);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeanceGetResponse> search(Long eventId, Instant dateFrom, Instant dateTo, Pageable pageable) {
        log.debug("seance.search params eventId={} dateFrom={} dateTo={} page={} size={} sort={}",
                eventId, dateFrom, dateTo,
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        boolean noFilters = (eventId == null && dateFrom == null && dateTo == null);

        Page<Seance> page;
        if (noFilters) {
            page = seanceRepository.findAll(pageable);
        } else {
            Specification<Seance> spec = Specification.where(null);

            if (eventId != null) {
                spec = spec.and((root, q, cb) -> cb.equal(root.get("event").get("id"), eventId));
            }
            if (dateFrom != null) {
                spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), dateFrom));
            }
            if (dateTo != null) {
                spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), dateTo));
            }

            page = seanceRepository.findAll(spec, pageable);
        }

        log.debug("seance.search.result totalElements={} totalPages={} pageNumber={}",
                page.getTotalElements(), page.getTotalPages(), page.getNumber());

        return page.map(seanceMapper::toGetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SeanceAvailabilityResponse availability(Long id) {
        log.debug("seance.availability.start seanceId={}", id);

        Seance seance = seanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Seance not found: " + id));

        Instant now = Instant.now();

        long heldCount = holdRepository.sumActiveQuantity(seance.getId(), HoldStatus.HELD, now);
        long soldCount = purchaseRepository.sumQuantityBySeanceAndStatus(seance.getId(), PurchaseStatus.SOLD);

        long capacity = seance.getCapacity();
        long available = capacity - soldCount - heldCount;

        if (available < 0) {
            log.warn("seance.availability.negative seanceId={} capacity={} sold={} held={} available={} now={}",
                    seance.getId(), capacity, soldCount, heldCount, available, now);
            available = 0;
        }

        log.info("seance.availability.ok seanceId={} capacity={} sold={} held={} available={} now={}",
                seance.getId(), capacity, soldCount, heldCount, available, now);

        return new SeanceAvailabilityResponse(
                seance.getId(),
                (int) capacity,
                soldCount,
                heldCount,
                available
        );
    }
}
