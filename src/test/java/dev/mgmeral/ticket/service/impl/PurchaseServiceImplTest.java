package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Hold;
import dev.mgmeral.ticket.entity.Payment;
import dev.mgmeral.ticket.entity.Purchase;
import dev.mgmeral.ticket.enums.HoldStatus;
import dev.mgmeral.ticket.enums.PaymentStatus;
import dev.mgmeral.ticket.enums.PurchaseStatus;
import dev.mgmeral.ticket.model.PurchaseCreateRequest;
import dev.mgmeral.ticket.model.PurchaseResponse;
import dev.mgmeral.ticket.repository.HoldRepository;
import dev.mgmeral.ticket.repository.PaymentRepository;
import dev.mgmeral.ticket.repository.PurchaseRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplTest {
    @Mock
    PurchaseRepository purchaseRepository;
    @Mock
    HoldRepository holdRepository;
    @Mock
    PaymentRepository paymentRepository;

    PurchaseServiceImpl service;

    @Captor
    ArgumentCaptor<Purchase> purchaseCaptor;
    @Captor
    ArgumentCaptor<Hold> holdCaptor;

    private static Payment payment(String ref, PaymentStatus status, String amount) {
        Payment p = new Payment();
        p.setPaymentRef(ref);
        p.setStatus(status);
        p.setAmount(new BigDecimal(amount));
        return p;
    }

    private static Hold hold(long id, long seanceId, long userId, int qty, HoldStatus status, Instant expiresAt) {
        Hold h = new Hold();
        h.setId(id);
        h.setSeanceId(seanceId);
        h.setUserId(userId);
        h.setQuantity(qty);
        h.setStatus(status);
        h.setExpiresAt(expiresAt);
        return h;
    }

    private static Purchase purchase(long id, String idem, String paymentRef, long holdId, long seanceId, long userId,
                                     int qty, String amount, PurchaseStatus status, Instant createdAt) {
        Purchase p = new Purchase();
        p.setId(id);
        p.setIdempotencyKey(idem);
        p.setPaymentRef(paymentRef);
        p.setHoldId(holdId);
        p.setSeanceId(seanceId);
        p.setUserId(userId);
        p.setQuantity(qty);
        p.setAmount(new BigDecimal(amount));
        p.setStatus(status);
        return p;
    }

    @BeforeEach
    void setup() {
        MeterRegistry registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        service = new PurchaseServiceImpl(purchaseRepository, holdRepository, paymentRepository, registry);
    }

    @Test
    void create_shouldReturnExisting_whenIdempotencyKeyAlreadyUsed() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-1");

        var existing = purchase(
                99L, "idem-1", "pay-1",
                1L, 3L, 2L,
                1, "100.00", PurchaseStatus.SOLD,
                Instant.parse("2028-01-01T00:00:00Z")
        );

        when(purchaseRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        var res = service.create(req);
        PurchaseResponse response = res.response();
        assertThat(response.purchaseId()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo(PurchaseStatus.SOLD);

        verify(purchaseRepository).findByIdempotencyKey("idem-1");
        verifyNoMoreInteractions(purchaseRepository);
        verifyNoInteractions(paymentRepository, holdRepository);
    }

    @Test
    void create_shouldThrowEntityNotFound_whenPaymentMissing() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-x");
        when(req.paymentRef()).thenReturn("p-ref");

        when(purchaseRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentRef("p-ref")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Payment not found: p-ref");

        verify(purchaseRepository).findByIdempotencyKey("idem-x");
        verify(paymentRepository).findByPaymentRef("p-ref");
        verifyNoMoreInteractions(purchaseRepository, paymentRepository);
        verifyNoInteractions(holdRepository);
    }

    @Test
    void create_shouldThrowIllegalArgument_whenPaymentNotAuthorized() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-x");
        when(req.paymentRef()).thenReturn("p-ref");

        when(purchaseRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(payment("p-ref", PaymentStatus.DECLINED, "100.00")));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment not authorized: p-ref");

        verify(purchaseRepository).findByIdempotencyKey("idem-x");
        verify(paymentRepository).findByPaymentRef("p-ref");
        verifyNoMoreInteractions(purchaseRepository, paymentRepository);
        verifyNoInteractions(holdRepository);
    }

    @Test
    void create_shouldThrowEntityNotFound_whenHoldMissing() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-x");
        when(req.paymentRef()).thenReturn("p-ref");
        when(req.holdId()).thenReturn(10L);

        when(purchaseRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(payment("p-ref", PaymentStatus.AUTHORIZED, "100.00")));
        when(holdRepository.findWithLockById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hold not found: 10");

        verify(purchaseRepository).findByIdempotencyKey("idem-x");
        verify(paymentRepository).findByPaymentRef("p-ref");
        verify(holdRepository).findWithLockById(10L);
        verifyNoMoreInteractions(purchaseRepository, paymentRepository, holdRepository);
    }

    @Test
    void create_shouldThrowIllegalArgument_whenHoldNotHeld() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-x");
        when(req.paymentRef()).thenReturn("p-ref");
        when(req.holdId()).thenReturn(10L);

        when(purchaseRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(payment("p-ref", PaymentStatus.AUTHORIZED, "100.00")));

        var h = hold(10L, 20L, 30L, 1, HoldStatus.RELEASED, Instant.now().plusSeconds(60));
        when(holdRepository.findWithLockById(10L)).thenReturn(Optional.of(h));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hold not active")
                .hasMessageContaining("status=" + HoldStatus.RELEASED);

        verify(purchaseRepository).findByIdempotencyKey("idem-x");
        verify(paymentRepository).findByPaymentRef("p-ref");
        verify(holdRepository).findWithLockById(10L);
        verifyNoMoreInteractions(purchaseRepository, paymentRepository, holdRepository);
    }

    @Test
    void create_shouldExpireHold_andThrow_whenHoldExpired() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-x");
        when(req.paymentRef()).thenReturn("p-ref");
        when(req.holdId()).thenReturn(10L);

        when(purchaseRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(payment("p-ref", PaymentStatus.AUTHORIZED, "100.00")));

        var h = hold(10L, 20L, 30L, 1, HoldStatus.HELD, Instant.now().minusSeconds(5));
        when(holdRepository.findWithLockById(10L)).thenReturn(Optional.of(h));

        when(holdRepository.save(any(Hold.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hold expired: 10");

        verify(holdRepository).save(holdCaptor.capture());
        var saved = holdCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(HoldStatus.EXPIRED);
        assertThat(saved.getReleasedAt()).isNotNull();

        verify(purchaseRepository).findByIdempotencyKey("idem-x");
        verify(paymentRepository).findByPaymentRef("p-ref");
        verify(holdRepository).findWithLockById(10L);
        verifyNoMoreInteractions(purchaseRepository, paymentRepository, holdRepository);
    }

    @Test
    void create_shouldThrowIllegalArgument_whenPaymentAmountMismatch() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-x");
        when(req.paymentRef()).thenReturn("p-ref");
        when(req.holdId()).thenReturn(10L);

        when(purchaseRepository.findByIdempotencyKey("idem-x")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(payment("p-ref", PaymentStatus.AUTHORIZED, "100.00")));

        var h = hold(10L, 20L, 30L, 2, HoldStatus.HELD, Instant.now().plusSeconds(60));
        when(holdRepository.findWithLockById(10L)).thenReturn(Optional.of(h));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment amount mismatch")
                .hasMessageContaining("expected=200")
                .hasMessageContaining("actual=100.00");

        verify(purchaseRepository).findByIdempotencyKey("idem-x");
        verify(paymentRepository).findByPaymentRef("p-ref");
        verify(holdRepository).findWithLockById(10L);
        verifyNoMoreInteractions(purchaseRepository, paymentRepository, holdRepository);
    }

    @Test
    void create_shouldSavePurchase_andConsumeHold_andReturnResponse() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-ok");
        when(req.paymentRef()).thenReturn("p-ref");
        when(req.holdId()).thenReturn(10L);

        when(purchaseRepository.findByIdempotencyKey("idem-ok")).thenReturn(Optional.empty());
        when(paymentRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(payment("p-ref", PaymentStatus.AUTHORIZED, "200.00")));

        var h = hold(10L, 20L, 30L, 2, HoldStatus.HELD, Instant.now().plusSeconds(60));
        when(holdRepository.findWithLockById(10L)).thenReturn(Optional.of(h));

        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(inv -> {
            Purchase p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });

        when(holdRepository.save(any(Hold.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service.create(req);
        PurchaseResponse response = res.response();
        assertThat(response.purchaseId()).isEqualTo(999L);
        assertThat(response.status()).isEqualTo(PurchaseStatus.SOLD);
        assertThat(response.amount()).isEqualByComparingTo("200.00");
        assertThat(response.paymentRef()).isEqualTo("p-ref");

        verify(purchaseRepository).save(purchaseCaptor.capture());
        var savedPurchase = purchaseCaptor.getValue();
        assertThat(savedPurchase.getHoldId()).isEqualTo(10L);
        assertThat(savedPurchase.getSeanceId()).isEqualTo(20L);
        assertThat(savedPurchase.getUserId()).isEqualTo(30L);
        assertThat(savedPurchase.getQuantity()).isEqualTo(2);
        assertThat(savedPurchase.getAmount()).isEqualByComparingTo("200.00");
        assertThat(savedPurchase.getPaymentRef()).isEqualTo("p-ref");
        assertThat(savedPurchase.getStatus()).isEqualTo(PurchaseStatus.SOLD);
        assertThat(savedPurchase.getIdempotencyKey()).isEqualTo("idem-ok");

        verify(holdRepository).save(holdCaptor.capture());
        var updatedHold = holdCaptor.getValue();
        assertThat(updatedHold.getStatus()).isEqualTo(HoldStatus.CONSUMED);
        assertThat(updatedHold.getReleasedAt()).isNotNull();

        verify(purchaseRepository).findByIdempotencyKey("idem-ok");
        verify(paymentRepository).findByPaymentRef("p-ref");
        verify(holdRepository).findWithLockById(10L);
        verifyNoMoreInteractions(purchaseRepository, paymentRepository, holdRepository);
    }

    @Test
    void create_shouldRecoverOnDuplicate_usingIdempotencyKeyLookup() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-dup");
        when(req.paymentRef()).thenReturn("p-ref");
        when(req.holdId()).thenReturn(10L);

        when(purchaseRepository.findByIdempotencyKey("idem-dup"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(purchase(
                        1L, "idem-dup", "p-ref",
                        10L, 20L, 30L, 1,
                        "100.00", PurchaseStatus.SOLD,
                        Instant.parse("2028-01-01T00:00:00Z")
                )));

        when(paymentRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(payment("p-ref", PaymentStatus.AUTHORIZED, "100.00")));

        var h = hold(10L, 20L, 30L, 1, HoldStatus.HELD, Instant.now().plusSeconds(60));
        when(holdRepository.findWithLockById(10L)).thenReturn(Optional.of(h));

        var dup = new DataIntegrityViolationException("dup");
        when(purchaseRepository.save(any(Purchase.class))).thenThrow(dup);

        var res = service.create(req);
        PurchaseResponse response = res.response();
        assertThat(response.purchaseId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(PurchaseStatus.SOLD);

        verify(purchaseRepository).save(purchaseCaptor.capture());
        assertThat(purchaseCaptor.getValue().getIdempotencyKey()).isEqualTo("idem-dup");

        verify(purchaseRepository, times(2)).findByIdempotencyKey("idem-dup");
        verify(purchaseRepository, never()).findByPaymentRef(anyString());
        verify(holdRepository, never()).save(any());

        verify(paymentRepository).findByPaymentRef("p-ref");
        verify(holdRepository).findWithLockById(10L);

        verifyNoMoreInteractions(purchaseRepository, paymentRepository, holdRepository);
    }

    @Test
    void create_shouldThrowConflictOnDuplicatePaymentRef_whenIdempotencyKeyNotFound() {
        var req = mock(PurchaseCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-dup2");
        when(req.paymentRef()).thenReturn("p-ref");
        when(req.holdId()).thenReturn(10L);

        when(purchaseRepository.findByIdempotencyKey("idem-dup2"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());

        when(paymentRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(payment("p-ref", PaymentStatus.AUTHORIZED, "100.00")));

        var h = hold(10L, 20L, 30L, 1, HoldStatus.HELD, Instant.now().plusSeconds(60));
        when(holdRepository.findWithLockById(10L)).thenReturn(Optional.of(h));

        when(purchaseRepository.save(any(Purchase.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        when(purchaseRepository.findByPaymentRef("p-ref"))
                .thenReturn(Optional.of(purchase(
                        2L, "idem-???", "p-ref",
                        10L, 20L, 30L, 1,
                        "100.00", PurchaseStatus.SOLD,
                        Instant.parse("2028-01-01T00:00:00Z")
                )));

        var ex = assertThrows(dev.mgmeral.ticket.exception.DuplicatePaymentRefException.class,
                () -> service.create(req));

        assertThat(ex.getExistingPurchaseId()).isEqualTo(2L);

        verify(purchaseRepository).save(any(Purchase.class));
        verify(purchaseRepository, times(2)).findByIdempotencyKey("idem-dup2");
        verify(purchaseRepository).findByPaymentRef("p-ref");

        verify(paymentRepository).findByPaymentRef("p-ref");
        verify(holdRepository).findWithLockById(10L);

        verify(holdRepository, never()).save(any());

        verifyNoMoreInteractions(purchaseRepository, paymentRepository, holdRepository);
    }

}
