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
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeanceServiceImplTest {

    @Mock
    SeanceRepository seanceRepository;
    @Mock
    EventRepository eventRepository;
    @Mock
    HoldRepository holdRepository;
    @Mock
    PurchaseRepository purchaseRepository;
    @Mock
    SeanceMapper seanceMapper;

    @InjectMocks
    SeanceServiceImpl service;

    @Captor
    ArgumentCaptor<Seance> seanceCaptor;
    @Captor
    ArgumentCaptor<Specification<Seance>> specCaptor;
    @Captor
    ArgumentCaptor<Instant> instantCaptor;

    @Test
    void create_shouldThrowEntityNotFound_whenEventMissing() {
        long eventId = 10L;
        var req = mock(SeanceCreateRequest.class);

        when(eventRepository.existsById(eq(eventId))).thenReturn(false);

        assertThatThrownBy(() -> service.create(eventId, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found: " + eventId);

        verify(eventRepository).existsById(eq(eventId));
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(seanceRepository, holdRepository, purchaseRepository, seanceMapper);
    }

    @Test
    void create_shouldMapSaveAndReturnResponse_whenEventExists() {
        long eventId = 10L;
        var req = mock(SeanceCreateRequest.class);

        when(eventRepository.existsById(eq(eventId))).thenReturn(true);

        Seance mapped = new Seance();
        Seance saved = new Seance();
        var resp = mock(SeanceCreateResponse.class);

        when(seanceMapper.toEntity(eq(eventId), eq(req))).thenReturn(mapped);
        when(seanceRepository.save(eq(mapped))).thenReturn(saved);
        when(seanceMapper.toCreateResponse(eq(saved))).thenReturn(resp);

        var result = service.create(eventId, req);

        assertThat(result).isSameAs(resp);

        verify(eventRepository).existsById(eq(eventId));
        verify(seanceMapper).toEntity(eq(eventId), eq(req));
        verify(seanceRepository).save(eq(mapped));
        verify(seanceMapper).toCreateResponse(eq(saved));
        verifyNoMoreInteractions(eventRepository, seanceRepository, seanceMapper);
        verifyNoInteractions(holdRepository, purchaseRepository);
    }

    @Test
    void getById_shouldReturnGetResponse_whenFound() {
        long id = 5L;
        Seance s = new Seance();
        var resp = mock(SeanceGetResponse.class);

        when(seanceRepository.findById(eq(id))).thenReturn(Optional.of(s));
        when(seanceMapper.toGetResponse(eq(s))).thenReturn(resp);

        var result = service.getById(id);

        assertThat(result).isSameAs(resp);

        verify(seanceRepository).findById(eq(id));
        verify(seanceMapper).toGetResponse(eq(s));
        verifyNoMoreInteractions(seanceRepository, seanceMapper);
        verifyNoInteractions(eventRepository, holdRepository, purchaseRepository);
    }

    @Test
    void getById_shouldThrowEntityNotFound_whenMissing() {
        long id = 404L;

        when(seanceRepository.findById(eq(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Seance not found: " + id);

        verify(seanceRepository).findById(eq(id));
        verifyNoMoreInteractions(seanceRepository);
        verifyNoInteractions(eventRepository, holdRepository, purchaseRepository, seanceMapper);
    }

    @Test
    void search_shouldCallFindAllPageable_whenAllFiltersNull_andMapResults() {
        Pageable pageable = PageRequest.of(0, 10);

        Seance s = new Seance();
        var page = new PageImpl<>(List.of(s), pageable, 1);

        when(seanceRepository.findAll(eq(pageable))).thenReturn(page);

        var mapped = mock(SeanceGetResponse.class);
        when(seanceMapper.toGetResponse(same(s))).thenReturn(mapped);

        var result = service.search(null, null, null, pageable);

        assertThat(result.getContent()).containsExactly(mapped);

        verify(seanceRepository).findAll(eq(pageable));
        verify(seanceRepository, never()).findAll(any(Specification.class), any(Pageable.class));

        verify(seanceMapper).toGetResponse(same(s));

        verifyNoMoreInteractions(seanceRepository, seanceMapper);
        verifyNoInteractions(eventRepository, holdRepository, purchaseRepository);
    }

    @Test
    void search_shouldBuildNonNullSpec_whenAnyFilterProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        var empty = Page.<Seance>empty(pageable);

        when(seanceRepository.findAll(Mockito.isA(Specification.class), eq(pageable))).thenReturn(empty);

        Instant from = Instant.parse("2028-01-01T00:00:00Z");
        Instant to = Instant.parse("2028-12-31T23:59:59Z");

        var result = service.search(10L, from, to, pageable);

        assertThat(result.getTotalElements()).isZero();

        verify(seanceRepository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();

        verifyNoMoreInteractions(seanceRepository);
        verifyNoInteractions(eventRepository, holdRepository, purchaseRepository, seanceMapper);
    }

    @Test
    void availability_shouldThrowEntityNotFound_whenSeanceMissing() {
        long id = 10L;
        when(seanceRepository.findById(eq(id))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.availability(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Seance not found: " + id);

        verify(seanceRepository).findById(eq(id));
        verifyNoMoreInteractions(seanceRepository);
        verifyNoInteractions(eventRepository, holdRepository, purchaseRepository, seanceMapper);
    }

    @Test
    void availability_shouldReturnCalculatedAvailability() {
        long id = 10L;

        Seance s = mock(Seance.class);
        when(s.getId()).thenReturn(id);
        when(s.getCapacity()).thenReturn(100);

        when(seanceRepository.findById(eq(id))).thenReturn(Optional.of(s));

        when(holdRepository.sumActiveQuantity(eq(id), eq(HoldStatus.HELD), instantCaptor.capture()))
                .thenReturn(12L);

        when(purchaseRepository.sumQuantityBySeanceAndStatus(eq(id), eq(PurchaseStatus.SOLD)))
                .thenReturn(30L);

        SeanceAvailabilityResponse res = service.availability(id);

        assertThat(res.seanceId()).isEqualTo(10L);
        assertThat(res.capacity()).isEqualTo(100);
        assertThat(res.soldCount()).isEqualTo(30);
        assertThat(res.heldCount()).isEqualTo(12);
        assertThat(res.availableCount()).isEqualTo(58);

        verify(seanceRepository).findById(eq(id));
        verify(holdRepository).sumActiveQuantity(eq(id), eq(HoldStatus.HELD), eq(instantCaptor.getValue()));
        verify(purchaseRepository).sumQuantityBySeanceAndStatus(eq(id), eq(PurchaseStatus.SOLD));

        verifyNoMoreInteractions(seanceRepository, holdRepository, purchaseRepository);
        verifyNoInteractions(eventRepository, seanceMapper);
    }

    @Test
    void availability_shouldClampAvailableToZero_whenNegative() {
        long id = 10L;

        Seance s = mock(Seance.class);
        when(s.getId()).thenReturn(id);
        when(s.getCapacity()).thenReturn(10);

        when(seanceRepository.findById(eq(id))).thenReturn(Optional.of(s));

        when(holdRepository.sumActiveQuantity(eq(id), eq(HoldStatus.HELD), instantCaptor.capture()))
                .thenReturn(5L);

        when(purchaseRepository.sumQuantityBySeanceAndStatus(eq(id), eq(PurchaseStatus.SOLD)))
                .thenReturn(9L);

        SeanceAvailabilityResponse res = service.availability(id);

        assertThat(res.availableCount()).isZero();

        verify(seanceRepository).findById(eq(id));
        verify(holdRepository).sumActiveQuantity(eq(id), eq(HoldStatus.HELD), eq(instantCaptor.getValue()));
        verify(purchaseRepository).sumQuantityBySeanceAndStatus(eq(id), eq(PurchaseStatus.SOLD));
        verifyNoMoreInteractions(seanceRepository, holdRepository, purchaseRepository);
        verifyNoInteractions(eventRepository, seanceMapper);
    }
}
