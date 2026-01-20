package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Hold;
import dev.mgmeral.ticket.entity.Payment;
import dev.mgmeral.ticket.entity.Purchase;
import dev.mgmeral.ticket.enums.HoldStatus;
import dev.mgmeral.ticket.enums.PaymentStatus;
import dev.mgmeral.ticket.enums.PurchaseStatus;
import dev.mgmeral.ticket.model.PurchaseCreateRequest;
import dev.mgmeral.ticket.model.PurchaseCreateResult;
import dev.mgmeral.ticket.model.PurchaseResponse;
import dev.mgmeral.ticket.repository.HoldRepository;
import dev.mgmeral.ticket.repository.PaymentRepository;
import dev.mgmeral.ticket.repository.PurchaseRepository;
import dev.mgmeral.ticket.service.PurchaseService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Transactional
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {
    private static final BigDecimal UNIT_PRICE = new BigDecimal("100.00");
    private final Counter purchaseCreated;
    private final Counter purchaseExisting;
    private final Counter purchaseFailed;
    private final Timer purchaseCreateTimer;

    private final PurchaseRepository purchaseRepository;
    private final HoldRepository holdRepository;
    private final PaymentRepository paymentRepository;

    public PurchaseServiceImpl(PurchaseRepository purchaseRepository,
                               HoldRepository holdRepository,
                               PaymentRepository paymentRepository,
                               MeterRegistry registry) {
        this.purchaseRepository = purchaseRepository;
        this.holdRepository = holdRepository;
        this.paymentRepository = paymentRepository;

        this.purchaseCreated = registry.counter("purchase_created_total");
        this.purchaseExisting = registry.counter("purchase_existing_total");
        this.purchaseFailed = registry.counter("purchase_failed_total");
        this.purchaseCreateTimer = registry.timer("purchase_create_seconds");
    }

    @Override
    public PurchaseCreateResult create(PurchaseCreateRequest request) {
        final String idemKey = request.idempotencyKey();
        final String paymentRef = request.paymentRef();
        final Long holdIdReq = request.holdId();

        log.info("order.create.start idemKey={} paymentRef={} holdId={}", idemKey, paymentRef, holdIdReq);

        return purchaseCreateTimer.record(() -> {
            try {
                var existingByKey = purchaseRepository.findByIdempotencyKey(idemKey);
                if (existingByKey.isPresent()) {
                    var ex = existingByKey.get();
                    purchaseExisting.increment();
                    log.info("order.idempotentHit orderId={} idemKey={} paymentRef={} holdId={} seanceId={}",
                            ex.getId(), ex.getIdempotencyKey(), ex.getPaymentRef(), ex.getHoldId(), ex.getSeanceId());
                    return PurchaseCreateResult.existing(toResponse(ex));
                }

                Instant now = Instant.now();

                Payment payment = paymentRepository.findByPaymentRef(paymentRef)
                        .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentRef));

                if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
                    log.warn("order.create.paymentNotAuthorized idemKey={} paymentRef={} status={}",
                            idemKey, paymentRef, payment.getStatus());
                    throw new IllegalArgumentException("Payment not authorized: " + paymentRef);
                }

                Hold hold = holdRepository.findWithLockById(holdIdReq)
                        .orElseThrow(() -> new EntityNotFoundException("Hold not found: " + holdIdReq));

                if (hold.getStatus() != HoldStatus.HELD) {
                    log.warn("order.create.holdNotHeld idemKey={} holdId={} status={} seanceId={} userId={}",
                            idemKey, hold.getId(), hold.getStatus(), hold.getSeanceId(), hold.getUserId());
                    throw new IllegalArgumentException("Hold not active. status=" + hold.getStatus());
                }

                if (hold.getExpiresAt() != null && hold.getExpiresAt().isBefore(now)) {
                    hold.setStatus(HoldStatus.EXPIRED);
                    hold.setReleasedAt(now);
                    holdRepository.save(hold);

                    log.warn("order.create.holdExpired idemKey={} holdId={} seanceId={} userId={} expiresAt={} now={}",
                            idemKey, hold.getId(), hold.getSeanceId(), hold.getUserId(), hold.getExpiresAt(), now);

                    throw new IllegalArgumentException("Hold expired: " + hold.getId());
                }

                BigDecimal expectedAmount = UNIT_PRICE.multiply(BigDecimal.valueOf(hold.getQuantity()));
                if (payment.getAmount().compareTo(expectedAmount) != 0) {
                    log.warn("order.create.amountMismatch idemKey={} paymentRef={} holdId={} expected={} actual={}",
                            idemKey, paymentRef, hold.getId(), expectedAmount, payment.getAmount());
                    throw new IllegalArgumentException(
                            "Payment amount mismatch. expected=" + expectedAmount + " actual=" + payment.getAmount()
                    );
                }

                Purchase purchase = Purchase.builder()
                        .holdId(hold.getId())
                        .seanceId(hold.getSeanceId())
                        .userId(hold.getUserId())
                        .quantity(hold.getQuantity())
                        .amount(expectedAmount)
                        .paymentRef(payment.getPaymentRef())
                        .status(PurchaseStatus.SOLD)
                        .idempotencyKey(idemKey)
                        .build();

                Purchase saved = purchaseRepository.save(purchase);

                hold.setStatus(HoldStatus.CONSUMED);
                hold.setReleasedAt(now);
                holdRepository.save(hold);

                purchaseCreated.increment();

                log.info("order.created orderId={} idemKey={} holdId={} seanceId={} userId={} paymentRef={} amount={} qty={}",
                        saved.getId(), idemKey, hold.getId(), hold.getSeanceId(), hold.getUserId(),
                        payment.getPaymentRef(), expectedAmount, hold.getQuantity());

                return PurchaseCreateResult.created(toResponse(saved));

            } catch (DataIntegrityViolationException ex) {
                var byKey = purchaseRepository.findByIdempotencyKey(idemKey);
                if (byKey.isPresent()) {
                    purchaseExisting.increment();
                    var p = byKey.get();
                    log.info("order.idempotentRecovered idemKey={} orderId={} paymentRef={}",
                            idemKey, p.getId(), p.getPaymentRef());
                    return PurchaseCreateResult.existing(toResponse(p));
                }

                var byPayment = purchaseRepository.findByPaymentRef(paymentRef);
                if (byPayment.isPresent()) {
                    purchaseFailed.increment();
                    log.warn("order.duplicatePaymentRef idemKey={} paymentRef={} existingOrderId={}",
                            idemKey, paymentRef, byPayment.get().getId());
                    throw new dev.mgmeral.ticket.exception.DuplicatePaymentRefException(byPayment.get().getId());
                }

                purchaseFailed.increment();
                log.error("order.create.dbFailure idemKey={} paymentRef={} holdId={} err={}",
                        idemKey, paymentRef, holdIdReq, ex.toString(), ex);
                throw new IllegalStateException("Purchase save failed", ex);

            } catch (RuntimeException e) {
                purchaseFailed.increment();
                log.error("order.create.failed idemKey={} paymentRef={} holdId={} err={}",
                        idemKey, paymentRef, holdIdReq, e.toString(), e);
                throw e;
            }
        });
    }

    private PurchaseResponse toResponse(Purchase p) {
        return new PurchaseResponse(
                p.getId(),
                p.getHoldId(),
                p.getUserId(),
                p.getSeanceId(),
                p.getQuantity(),
                p.getAmount(),
                p.getPaymentRef(),
                p.getStatus(),
                p.getCreatedAt()
        );
    }
}
