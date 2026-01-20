package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Hold;
import dev.mgmeral.ticket.entity.Seance;
import dev.mgmeral.ticket.enums.HoldStatus;
import dev.mgmeral.ticket.model.HoldCreateRequest;
import dev.mgmeral.ticket.model.HoldResponse;
import dev.mgmeral.ticket.repository.HoldRepository;
import dev.mgmeral.ticket.repository.PurchaseRepository;
import dev.mgmeral.ticket.repository.SeanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static dev.mgmeral.ticket.enums.PurchaseStatus.SOLD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldServiceImplTest {

    @Mock
    HoldRepository holdRepository;
    @Mock
    SeanceRepository seanceRepository;
    @Mock
    PurchaseRepository purchaseRepository;

    @InjectMocks
    HoldServiceImpl service;

    @Captor
    ArgumentCaptor<Hold> holdCaptor;
    @Captor
    ArgumentCaptor<Instant> instantCaptor;

    @Test
    void create_shouldReturnExistingHold_whenIdempotencyKeyAlreadyUsed() {
        var req = mock(HoldCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-1");

        Hold existing = Hold.builder()
                .id(11L)
                .userId(7L)
                .seanceId(99L)
                .quantity(2)
                .status(HoldStatus.HELD)
                .idempotencyKey("idem-1")
                .expiresAt(Instant.parse("2028-01-01T00:05:00Z"))
                .build();

        when(holdRepository.findByIdempotencyKey(eq("idem-1"))).thenReturn(Optional.of(existing));

        HoldResponse res = service.create(req);

        assertThat(res.id()).isEqualTo(11L);
        assertThat(res.userId()).isEqualTo(7L);
        assertThat(res.seanceId()).isEqualTo(99L);
        assertThat(res.quantity()).isEqualTo(2);
        assertThat(res.status()).isEqualTo(HoldStatus.HELD);
        assertThat(res.expiresAt()).isEqualTo(existing.getExpiresAt());
        assertThat(res.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(200));

        verify(holdRepository).findByIdempotencyKey(eq("idem-1"));
        verifyNoMoreInteractions(holdRepository);
        verifyNoInteractions(seanceRepository, purchaseRepository);
    }

    @Test
    void create_shouldCreateHold_whenCapacityIsEnough_andReturnResponse() {
        var req = mock(HoldCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-2");
        when(req.seanceId()).thenReturn(100L);
        when(req.userId()).thenReturn(55L);
        when(req.quantity()).thenReturn(3);

        when(holdRepository.findByIdempotencyKey(eq("idem-2"))).thenReturn(Optional.empty());

        Seance seance = mock(Seance.class);
        when(seance.getId()).thenReturn(100L);
        when(seance.getCapacity()).thenReturn(10);

        when(seanceRepository.findWithLockById(eq(100L))).thenReturn(Optional.of(seance));

        when(holdRepository.sumActiveQuantity(eq(100L), eq(HoldStatus.HELD), instantCaptor.capture()))
                .thenReturn(2L);
        when(purchaseRepository.sumQuantityBySeanceAndStatus(eq(100L), eq(SOLD)))
                .thenReturn(4L);

        when(holdRepository.save(holdCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        HoldResponse res = service.create(req);

        assertThat(res.userId()).isEqualTo(55L);
        assertThat(res.seanceId()).isEqualTo(100L);
        assertThat(res.quantity()).isEqualTo(3);
        assertThat(res.status()).isEqualTo(HoldStatus.HELD);
        assertThat(res.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(300));
        assertThat(res.expiresAt()).isNotNull();

        Hold created = holdCaptor.getValue();
        assertThat(created.getUserId()).isEqualTo(55L);
        assertThat(created.getSeanceId()).isEqualTo(100L);
        assertThat(created.getQuantity()).isEqualTo(3);
        assertThat(created.getStatus()).isEqualTo(HoldStatus.HELD);
        assertThat(created.getIdempotencyKey()).isEqualTo("idem-2");
        assertThat(created.getExpiresAt()).isNotNull();

        verify(holdRepository).findByIdempotencyKey(eq("idem-2"));
        verify(seanceRepository).findWithLockById(eq(100L));
        verify(holdRepository).sumActiveQuantity(eq(100L), eq(HoldStatus.HELD), eq(instantCaptor.getValue()));
        verify(purchaseRepository).sumQuantityBySeanceAndStatus(eq(100L), eq(SOLD));
        verify(holdRepository).save(eq(created));
        verifyNoMoreInteractions(holdRepository, seanceRepository, purchaseRepository);
    }

    @Test
    void create_shouldThrowEntityNotFound_whenSeanceMissing() {
        var req = mock(HoldCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-x");
        when(req.seanceId()).thenReturn(999L);

        when(holdRepository.findByIdempotencyKey(eq("idem-x"))).thenReturn(Optional.empty());
        when(seanceRepository.findWithLockById(eq(999L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Seance not found: 999");

        verify(holdRepository).findByIdempotencyKey(eq("idem-x"));
        verify(seanceRepository).findWithLockById(eq(999L));
        verifyNoMoreInteractions(holdRepository, seanceRepository);
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void create_shouldThrowIllegalArgumentException_whenInsufficientCapacity() {
        var req = mock(HoldCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-cap");
        when(req.seanceId()).thenReturn(10L);
        when(req.quantity()).thenReturn(6);

        when(holdRepository.findByIdempotencyKey(eq("idem-cap"))).thenReturn(Optional.empty());

        Seance seance = mock(Seance.class);
        when(seance.getId()).thenReturn(10L);
        when(seance.getCapacity()).thenReturn(10);
        when(seanceRepository.findWithLockById(eq(10L))).thenReturn(Optional.of(seance));

        when(holdRepository.sumActiveQuantity(eq(10L), eq(HoldStatus.HELD), any(Instant.class)))
                .thenReturn(3L);
        when(purchaseRepository.sumQuantityBySeanceAndStatus(eq(10L), eq(SOLD)))
                .thenReturn(5L);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient capacity")
                .hasMessageContaining("available=2");

        verify(holdRepository).findByIdempotencyKey(eq("idem-cap"));
        verify(seanceRepository).findWithLockById(eq(10L));
        verify(holdRepository).sumActiveQuantity(eq(10L), eq(HoldStatus.HELD), any(Instant.class));
        verify(purchaseRepository).sumQuantityBySeanceAndStatus(eq(10L), eq(SOLD));
        verify(holdRepository, never()).save(any());
        verifyNoMoreInteractions(holdRepository, seanceRepository, purchaseRepository);
    }


    @Test
    void create_shouldRecoverFromDuplicateKey_andReturnExistingHold() {
        var req = mock(HoldCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-dup");
        when(req.seanceId()).thenReturn(77L);
        when(req.userId()).thenReturn(9L);
        when(req.quantity()).thenReturn(1);

        Seance seance = mock(Seance.class);
        when(seance.getId()).thenReturn(77L);
        when(seance.getCapacity()).thenReturn(100);
        when(seanceRepository.findWithLockById(eq(77L))).thenReturn(Optional.of(seance));

        when(holdRepository.sumActiveQuantity(eq(77L), eq(HoldStatus.HELD), any(Instant.class)))
                .thenReturn(0L);
        when(purchaseRepository.sumQuantityBySeanceAndStatus(eq(77L), eq(SOLD)))
                .thenReturn(0L);

        var dup = new DataIntegrityViolationException("dup");
        when(holdRepository.save(any(Hold.class))).thenThrow(dup);

        Hold existing = Hold.builder()
                .id(500L)
                .userId(9L)
                .seanceId(77L)
                .quantity(1)
                .status(HoldStatus.HELD)
                .idempotencyKey("idem-dup")
                .expiresAt(Instant.parse("2028-01-01T00:05:00Z"))
                .build();

        when(holdRepository.findByIdempotencyKey(eq("idem-dup")))
                .thenReturn(Optional.empty(), Optional.of(existing));

        HoldResponse res = service.create(req);

        assertThat(res.id()).isEqualTo(500L);
        assertThat(res.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));

        verify(holdRepository, times(2)).findByIdempotencyKey(eq("idem-dup"));
        verify(seanceRepository).findWithLockById(eq(77L));
        verify(holdRepository).sumActiveQuantity(eq(77L), eq(HoldStatus.HELD), any(Instant.class));
        verify(purchaseRepository).sumQuantityBySeanceAndStatus(eq(77L), eq(SOLD));

        verify(holdRepository).save(holdCaptor.capture());
        assertThat(holdCaptor.getValue().getIdempotencyKey()).isEqualTo("idem-dup");

        verifyNoMoreInteractions(holdRepository, seanceRepository, purchaseRepository);
    }


    @Test
    void create_shouldRethrowDuplicateKey_whenSecondLookupMissing() {
        var req = mock(HoldCreateRequest.class);
        when(req.idempotencyKey()).thenReturn("idem-dup2");
        when(req.seanceId()).thenReturn(88L);
        when(req.userId()).thenReturn(9L);
        when(req.quantity()).thenReturn(1);

        Seance seance = mock(Seance.class);
        when(seance.getId()).thenReturn(88L);
        when(seance.getCapacity()).thenReturn(100);
        when(seanceRepository.findWithLockById(eq(88L))).thenReturn(Optional.of(seance));

        when(holdRepository.sumActiveQuantity(eq(88L), eq(HoldStatus.HELD), instantCaptor.capture()))
                .thenReturn(0L);
        when(purchaseRepository.sumQuantityBySeanceAndStatus(eq(88L), eq(SOLD)))
                .thenReturn(0L);

        var dup = new DataIntegrityViolationException("dup");
        when(holdRepository.save(holdCaptor.capture())).thenThrow(dup);

        when(holdRepository.findByIdempotencyKey(eq("idem-dup2"))).thenReturn(Optional.empty(), Optional.empty());
        assertThatThrownBy(() -> service.create(req))
                .isSameAs(dup);

        verify(seanceRepository).findWithLockById(eq(88L));
        verify(holdRepository).sumActiveQuantity(eq(88L), eq(HoldStatus.HELD), eq(instantCaptor.getValue()));
        verify(purchaseRepository).sumQuantityBySeanceAndStatus(eq(88L), eq(SOLD));
        verify(holdRepository).save(eq(holdCaptor.getValue()));
        verify(holdRepository, times(2)).findByIdempotencyKey(eq("idem-dup2"));
        verifyNoMoreInteractions(holdRepository, seanceRepository, purchaseRepository);
    }

    @Test
    void getById_shouldReturnResponse_whenFound() {
        Hold h = Hold.builder()
                .id(1L)
                .userId(2L)
                .seanceId(3L)
                .quantity(4)
                .status(HoldStatus.HELD)
                .expiresAt(Instant.parse("2028-01-01T00:05:00Z"))
                .build();

        when(holdRepository.findById(eq(1L))).thenReturn(Optional.of(h));

        HoldResponse res = service.getById(1L);

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.totalPrice()).isEqualByComparingTo(BigDecimal.valueOf(400));

        verify(holdRepository).findById(eq(1L));
        verifyNoMoreInteractions(holdRepository);
        verifyNoInteractions(seanceRepository, purchaseRepository);
    }

    @Test
    void getById_shouldThrowEntityNotFound_whenMissing() {
        when(holdRepository.findById(eq(404L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(404L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hold not found: 404");

        verify(holdRepository).findById(eq(404L));
        verifyNoMoreInteractions(holdRepository);
        verifyNoInteractions(seanceRepository, purchaseRepository);
    }

    @Test
    void release_shouldThrowEntityNotFound_whenMissing() {
        when(holdRepository.findWithLockById(eq(9L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.release(9L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Hold not found: 9");

        verify(holdRepository).findWithLockById(eq(9L));
        verifyNoMoreInteractions(holdRepository);
        verifyNoInteractions(seanceRepository, purchaseRepository);
    }

    @Test
    void release_shouldDoNothing_whenStatusIsNotHELD() {
        Hold h = Hold.builder()
                .id(1L)
                .status(HoldStatus.RELEASED)
                .build();

        when(holdRepository.findWithLockById(eq(1L))).thenReturn(Optional.of(h));

        service.release(1L);

        verify(holdRepository).findWithLockById(eq(1L));
        verify(holdRepository, never()).save(any(Hold.class));
        verifyNoMoreInteractions(holdRepository);
        verifyNoInteractions(seanceRepository, purchaseRepository);
    }

    @Test
    void release_shouldSetReleased_andSave_whenStatusIsHELD() {
        Hold h = Hold.builder()
                .id(1L)
                .status(HoldStatus.HELD)
                .build();

        when(holdRepository.findWithLockById(eq(1L))).thenReturn(Optional.of(h));
        when(holdRepository.save(any(Hold.class))).thenAnswer(inv -> inv.getArgument(0));

        service.release(1L);

        verify(holdRepository).findWithLockById(eq(1L));
        verify(holdRepository).save(holdCaptor.capture());

        Hold saved = holdCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(HoldStatus.RELEASED);
        assertThat(saved.getReleasedAt()).isNotNull();

        verifyNoMoreInteractions(holdRepository);
        verifyNoInteractions(seanceRepository, purchaseRepository);
    }

}
