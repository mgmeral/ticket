package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Hold;
import dev.mgmeral.ticket.entity.Seance;
import dev.mgmeral.ticket.enums.HoldStatus;
import dev.mgmeral.ticket.model.HoldCreateRequest;
import dev.mgmeral.ticket.model.HoldResponse;
import dev.mgmeral.ticket.repository.HoldRepository;
import dev.mgmeral.ticket.repository.PurchaseRepository;
import dev.mgmeral.ticket.repository.SeanceRepository;
import dev.mgmeral.ticket.service.HoldService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static dev.mgmeral.ticket.enums.PurchaseStatus.SOLD;

@Service
@Transactional
@Slf4j
public class HoldServiceImpl implements HoldService {

    private static final Duration HOLD_TTL = Duration.ofMinutes(5);
    private static final BigDecimal UNIT_PRICE = BigDecimal.valueOf(100);

    private final HoldRepository holdRepository;
    private final SeanceRepository seanceRepository;
    private final PurchaseRepository purchaseRepository;

    public HoldServiceImpl(HoldRepository holdRepository, SeanceRepository seanceRepository, PurchaseRepository purchaseRepository) {
        this.holdRepository = holdRepository;
        this.seanceRepository = seanceRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Override
    public HoldResponse create(HoldCreateRequest request) {
        Instant now = Instant.now();

        log.info("hold.create.start seanceId={} userId={} qty={} idemKey={}",
                request.seanceId(), request.userId(), request.quantity(), request.idempotencyKey());

        var existing = holdRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            Hold h = existing.get();
            boolean expiredNow = expireIfNeeded(h, now);
            if (expiredNow) {
                holdRepository.save(h);
                log.info("hold.create.idempotentExpired holdId={} seanceId={} userId={} expiresAt={} now={}",
                        h.getId(), h.getSeanceId(), h.getUserId(), h.getExpiresAt(), now);
            } else {
                log.info("hold.create.idempotentHit holdId={} seanceId={} userId={} status={} expiresAt={}",
                        h.getId(), h.getSeanceId(), h.getUserId(), h.getStatus(), h.getExpiresAt());
            }
            return toResponse(h);
        }


        Seance seance = seanceRepository.findWithLockById(request.seanceId())
                .orElseThrow(() -> new EntityNotFoundException("Seance not found: " + request.seanceId()));

        long activeHeld = holdRepository.sumActiveQuantity(seance.getId(), HoldStatus.HELD, now);
        long soldCount = purchaseRepository.sumQuantityBySeanceAndStatus(seance.getId(), SOLD);

        long available = (long) seance.getCapacity() - soldCount - activeHeld;

        log.debug("hold.create.availability seanceId={} capacity={} sold={} activeHeld={} available={}",
                seance.getId(), seance.getCapacity(), soldCount, activeHeld, available);

        if (request.quantity() > available) {
            log.warn("hold.create.insufficientCapacity seanceId={} requested={} available={} userId={}",
                    seance.getId(), request.quantity(), available, request.userId());
            throw new IllegalArgumentException("Insufficient capacity. available=" + available);
        }

        Hold hold = Hold.builder()
                .userId(request.userId())
                .seanceId(seance.getId())
                .quantity(request.quantity())
                .status(HoldStatus.HELD)
                .idempotencyKey(request.idempotencyKey())
                .expiresAt(now.plus(HOLD_TTL))
                .build();

        try {
            Hold saved = holdRepository.save(hold);

            log.info("hold.created holdId={} seanceId={} userId={} qty={} expiresAt={}",
                    saved.getId(), saved.getSeanceId(), saved.getUserId(), saved.getQuantity(), saved.getExpiresAt());

            return toResponse(saved);

        } catch (DataIntegrityViolationException dupKey) {
            Hold same = holdRepository.findByIdempotencyKey(request.idempotencyKey())
                    .orElseThrow(() -> dupKey);

            boolean expiredNow = expireIfNeeded(same, now);
            if (expiredNow) {
                holdRepository.save(same);
            }

            log.info("hold.create.dupRecovered holdId={} seanceId={} userId={} status={} expiresAt={}",
                    same.getId(), same.getSeanceId(), same.getUserId(), same.getStatus(), same.getExpiresAt());

            return toResponse(same);
        }
    }

    @Override
    public HoldResponse getById(Long holdId) {
        Instant now = Instant.now();
        Hold hold = holdRepository.findById(holdId)
                .orElseThrow(() -> new EntityNotFoundException("Hold not found: " + holdId));

        boolean expiredNow = expireIfNeeded(hold, now);
        if (expiredNow) {
            holdRepository.save(hold);
            log.info("hold.get.expiredLazy holdId={} seanceId={} userId={} now={}",
                    hold.getId(), hold.getSeanceId(), hold.getUserId(), now);
        } else {
            log.debug("hold.get.ok holdId={} seanceId={} userId={} status={} expiresAt={}",
                    hold.getId(), hold.getSeanceId(), hold.getUserId(), hold.getStatus(), hold.getExpiresAt());
        }

        return toResponse(hold);
    }

    @Override
    public void release(Long holdId) {
        Instant now = Instant.now();
        Hold hold = holdRepository.findWithLockById(holdId)
                .orElseThrow(() -> new EntityNotFoundException("Hold not found: " + holdId));

        boolean expiredNow = expireIfNeeded(hold, now);
        if (expiredNow) {
            holdRepository.save(hold);
            log.info("hold.release.expiredLazy holdId={} seanceId={} userId={} now={}",
                    hold.getId(), hold.getSeanceId(), hold.getUserId(), now);
            return;
        }

        if (hold.getStatus() != HoldStatus.HELD) {
            log.info("hold.release.noop holdId={} status={} seanceId={} userId={}",
                    holdId, hold.getStatus(), hold.getSeanceId(), hold.getUserId());
            return;
        }

        hold.setStatus(HoldStatus.RELEASED);
        hold.setReleasedAt(now);
        holdRepository.save(hold);

        log.info("hold.released holdId={} seanceId={} userId={} at={}",
                holdId, hold.getSeanceId(), hold.getUserId(), now);
    }

    @Override
    public boolean expireIfNeeded(Hold hold, Instant now) {
        if (hold.getStatus() == HoldStatus.HELD
                && hold.getExpiresAt() != null
                && !hold.getExpiresAt().isAfter(now)) {

            hold.setStatus(HoldStatus.EXPIRED);

            if (hold.getReleasedAt() == null) {
                hold.setReleasedAt(now);
            }
            return true;
        }
        return false;
    }

    private HoldResponse toResponse(Hold h) {
        BigDecimal total = UNIT_PRICE.multiply(BigDecimal.valueOf(h.getQuantity()));
        return new HoldResponse(
                h.getId(),
                h.getUserId(),
                h.getSeanceId(),
                h.getQuantity(),
                h.getStatus(),
                h.getExpiresAt(),
                total
        );
    }
}
