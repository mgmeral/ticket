package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Event;
import dev.mgmeral.ticket.enums.EventType;
import dev.mgmeral.ticket.mapper.EventMapper;
import dev.mgmeral.ticket.model.*;
import dev.mgmeral.ticket.repository.EventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
class EventServiceImplTest {

    @Mock
    EventRepository eventRepository;
    @Mock
    EventMapper eventMapper;
    @Mock
    dev.mgmeral.ticket.repository.PerformerRepository performerRepository;

    @InjectMocks
    EventServiceImpl service;
    @Captor
    ArgumentCaptor<Specification<Event>> specCaptor;

    @Test
    void create_shouldSaveMappedEntity_andReturnCreateResponse() {
        var req = mock(EventCreateRequest.class);
        var mapped = new Event();
        var saved = new Event();
        var resp = mock(EventCreateResponse.class);

        when(eventMapper.toEntity(req)).thenReturn(mapped);
        when(eventRepository.save(mapped)).thenReturn(saved);
        when(eventMapper.toCreateResponse(saved)).thenReturn(resp);

        var result = service.create(req);

        assertThat(result).isSameAs(resp);
        verify(eventMapper).toEntity(req);
        verify(eventRepository).save(mapped);
        verify(eventMapper).toCreateResponse(saved);
        verifyNoMoreInteractions(eventRepository, eventMapper);
    }

    @Test
    void getById_shouldReturnGetResponse_whenFound() {
        long id = 10L;
        var event = new Event();
        var resp = mock(EventGetResponse.class);

        when(eventRepository.findById(id)).thenReturn(Optional.of(event));
        when(eventMapper.toGetResponse(event)).thenReturn(resp);

        var result = service.getById(id);

        assertThat(result).isSameAs(resp);
        verify(eventRepository).findById(id);
        verify(eventMapper).toGetResponse(event);
        verifyNoMoreInteractions(eventRepository, eventMapper);
    }

    @Test
    void getById_shouldThrowEntityNotFound_whenMissing() {
        long id = 404L;
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found: " + id);

        verify(eventRepository).findById(id);
        verifyNoInteractions(eventMapper);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void update_shouldUpdateEntity_saveAndReturnUpdateResponse_whenFound() {
        long id = 7L;
        var req = mock(EventUpdateRequest.class);

        var existing = new Event();
        var saved = new Event();
        var resp = mock(EventUpdateResponse.class);

        when(eventRepository.findById(id)).thenReturn(Optional.of(existing));
        when(eventRepository.save(existing)).thenReturn(saved);
        when(eventMapper.toUpdateResponse(saved)).thenReturn(resp);

        var result = service.update(id, req);

        assertThat(result).isSameAs(resp);
        verify(eventRepository).findById(id);
        verify(eventMapper).updateEntity(existing, req);
        verify(eventRepository).save(existing);
        verify(eventMapper).toUpdateResponse(saved);
        verifyNoMoreInteractions(eventRepository, eventMapper);
    }

    @Test
    void update_shouldThrowEntityNotFound_whenMissing() {
        long id = 99L;
        var req = mock(EventUpdateRequest.class);
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found: " + id);

        verify(eventRepository).findById(id);
        verifyNoInteractions(eventMapper);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void delete_shouldDeleteById_whenExists() {
        long id = 1L;
        when(eventRepository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(eventRepository).existsById(id);
        verify(eventRepository).deleteById(id);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventMapper);
    }

    @Test
    void delete_shouldThrowEntityNotFound_whenNotExists() {
        long id = 2L;
        when(eventRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event not found: " + id);

        verify(eventRepository).existsById(id);
        verify(eventRepository, never()).deleteById(id);
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventMapper);
    }

    @Test
    void search_shouldPassNullSpec_whenAllFiltersEmpty_andMapResults() {
        Pageable pageable = PageRequest.of(0, 10);

        var e1 = new Event();
        var page = new PageImpl<>(List.of(e1), pageable, 1);

        when(eventRepository.findAll(specCaptor.capture(), eq(pageable))).thenReturn(page);

        var resp = mock(EventSearchResponse.class);
        when(eventMapper.toSearchResponse(e1)).thenReturn(resp);

        Page<EventSearchResponse> result = service.search("   ", "   ", null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(resp);

        assertThat(specCaptor.getValue()).isNotNull();

        verify(eventRepository).findAll(specCaptor.getValue(), pageable);
        verify(eventMapper).toSearchResponse(e1);
        verifyNoMoreInteractions(eventRepository, eventMapper);
    }

    @Test
    void search_shouldTrimUppercaseType_andBuildNonNullSpec_whenTypeProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        var page = Page.<Event>empty(pageable);

        when(eventRepository.findAll(specCaptor.capture(), eq(pageable))).thenReturn(page);

        Page<EventSearchResponse> result =
                service.search("  " + EventType.values()[0].name().toLowerCase() + "  ", null, null, null, pageable);

        assertThat(result.getTotalElements()).isZero();

        assertThat(specCaptor.getValue()).isNotNull();

        verify(eventRepository).findAll(specCaptor.getValue(), pageable);
        verifyNoInteractions(eventMapper);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void search_shouldTrimName_andBuildNonNullSpec_whenNameProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        var page = Page.<Event>empty(pageable);

        when(eventRepository.findAll(specCaptor.capture(), eq(pageable))).thenReturn(page);

        service.search(null, "   Rock Fest   ", null, null, pageable);

        assertThat(specCaptor.getValue()).isNotNull();
        verify(eventRepository).findAll(specCaptor.getValue(), pageable);
        verifyNoInteractions(eventMapper);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void search_shouldBuildNonNullSpec_whenStartFromProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        var page = Page.<Event>empty(pageable);

        when(eventRepository.findAll(specCaptor.capture(), eq(pageable))).thenReturn(page);

        service.search(null, null, Instant.parse("2028-01-01T00:00:00Z"), null, pageable);

        assertThat(specCaptor.getValue()).isNotNull();
        verify(eventRepository).findAll(specCaptor.getValue(), pageable);
        verifyNoInteractions(eventMapper);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void search_shouldBuildNonNullSpec_whenStartToProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        var page = Page.<Event>empty(pageable);

        when(eventRepository.findAll(specCaptor.capture(), eq(pageable))).thenReturn(page);

        service.search(null, null, null, Instant.parse("2028-12-31T23:59:59Z"), pageable);

        assertThat(specCaptor.getValue()).isNotNull();
        verify(eventRepository).findAll(specCaptor.getValue(), pageable);
        verifyNoInteractions(eventMapper);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void search_shouldThrowIllegalArgumentException_whenTypeInvalid_andNotCallRepo() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.search("___bad_enum___", null, null, null, pageable))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(eventRepository, eventMapper);
    }
}
