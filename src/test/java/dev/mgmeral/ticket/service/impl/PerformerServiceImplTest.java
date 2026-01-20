package dev.mgmeral.ticket.service.impl;

import dev.mgmeral.ticket.entity.Performer;
import dev.mgmeral.ticket.model.PerformerCreateRequest;
import dev.mgmeral.ticket.model.PerformerResponse;
import dev.mgmeral.ticket.model.PerformerUpdateRequest;
import dev.mgmeral.ticket.repository.PerformerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformerServiceImplTest {

    @Mock
    PerformerRepository performerRepository;

    PerformerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PerformerServiceImpl(performerRepository);
    }

    @Test
    void create_shouldThrow_whenPerformerAlreadyExists() {
        var req = new PerformerCreateRequest("Sezen Aksu", "SINGER", "Legend");
        when(performerRepository.existsByNameIgnoreCase(eq("Sezen Aksu"))).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Performer already exists");

        verify(performerRepository).existsByNameIgnoreCase(eq("Sezen Aksu"));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void create_shouldSaveAndReturnResponse_whenNameIsUnique() {
        var req = new PerformerCreateRequest("Tarkan", "SINGER", "Pop star");
        when(performerRepository.existsByNameIgnoreCase(eq("Tarkan"))).thenReturn(false);

        var saved = new Performer();
        saved.setId(10L);
        saved.setName("Tarkan");
        saved.setRole("SINGER");
        saved.setDescription("Pop star");

        ArgumentCaptor<Performer> captor = ArgumentCaptor.forClass(Performer.class);
        when(performerRepository.save(captor.capture())).thenReturn(saved);

        PerformerResponse res = service.create(req);

        assertThat(res.id()).isEqualTo(10L);
        assertThat(res.name()).isEqualTo("Tarkan");
        assertThat(res.role()).isEqualTo("SINGER");
        assertThat(res.description()).isEqualTo("Pop star");

        Performer toSave = captor.getValue();
        assertThat(toSave.getId()).as("ID set edilmemeli (DB verir)").isNull();
        assertThat(toSave.getName()).isEqualTo("Tarkan");
        assertThat(toSave.getRole()).isEqualTo("SINGER");
        assertThat(toSave.getDescription()).isEqualTo("Pop star");

        verify(performerRepository).existsByNameIgnoreCase(eq("Tarkan"));
        verify(performerRepository).save(any(Performer.class));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void getById_shouldThrow_whenNotFound() {
        when(performerRepository.findById(eq(99L))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Performer not found: 99");

        verify(performerRepository).findById(eq(99L));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void getById_shouldReturnResponse_whenFound() {
        var p = new Performer();
        p.setId(5L);
        p.setName("Cem Yılmaz");
        p.setRole("COMEDIAN");
        p.setDescription("Stand-up");

        when(performerRepository.findById(eq(5L))).thenReturn(Optional.of(p));

        PerformerResponse res = service.getById(5L);

        assertThat(res.id()).isEqualTo(5L);
        assertThat(res.name()).isEqualTo("Cem Yılmaz");
        assertThat(res.role()).isEqualTo("COMEDIAN");
        assertThat(res.description()).isEqualTo("Stand-up");

        verify(performerRepository).findById(eq(5L));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void list_shouldCallFindAll_whenNameIsNullOrBlank() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

        var p1 = new Performer();
        p1.setId(1L);
        p1.setName("A");
        p1.setRole("R");
        p1.setDescription("D1");

        var p2 = new Performer();
        p2.setId(2L);
        p2.setName("B");
        p2.setRole("R");
        p2.setDescription("D2");

        when(performerRepository.findAll(eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));

        Page<PerformerResponse> res1 = service.list(null, pageable);
        Page<PerformerResponse> res2 = service.list("   ", pageable);

        assertThat(res1.getTotalElements()).isEqualTo(2);
        assertThat(res1.getContent()).extracting(PerformerResponse::id).containsExactly(1L, 2L);

        assertThat(res2.getTotalElements()).isEqualTo(2);

        verify(performerRepository, times(2)).findAll(eq(pageable));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void list_shouldTrimAndSearch_whenNameProvided() {
        Pageable pageable = PageRequest.of(0, 5);
        String rawName = "  tar  ";

        var p = new Performer();
        p.setId(7L);
        p.setName("Tarkan");
        p.setRole("SINGER");
        p.setDescription("Pop");

        when(performerRepository.findByNameContainingIgnoreCase(eq("tar"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(p), pageable, 1));

        Page<PerformerResponse> page = service.list(rawName, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).name()).isEqualTo("Tarkan");

        verify(performerRepository).findByNameContainingIgnoreCase(eq("tar"), eq(pageable));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void update_shouldThrow_whenNotFound() {
        when(performerRepository.findById(eq(123L))).thenReturn(Optional.empty());
        var req = new PerformerUpdateRequest("New", "ROLE", "Desc");
        assertThatThrownBy(() -> service.update(123L, req))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Performer not found: 123");

        verify(performerRepository).findById(eq(123L));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void update_shouldModifyAndSave_whenFound() {
        var existing = new Performer();
        existing.setId(12L);
        existing.setName("Old");
        existing.setRole("OLD_ROLE");
        existing.setDescription("Old desc");

        when(performerRepository.findById(eq(12L))).thenReturn(Optional.of(existing));
        when(performerRepository.save(any(Performer.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new PerformerUpdateRequest("New Name", "NEW_ROLE", "New desc");

        PerformerResponse res = service.update(12L, req);

        assertThat(res.id()).isEqualTo(12L);
        assertThat(res.name()).isEqualTo("New Name");
        assertThat(res.role()).isEqualTo("NEW_ROLE");
        assertThat(res.description()).isEqualTo("New desc");

        verify(performerRepository).findById(eq(12L));
        verify(performerRepository).save(same(existing));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void delete_shouldThrow_whenNotFound() {
        when(performerRepository.existsById(eq(55L))).thenReturn(false);

        assertThatThrownBy(() -> service.delete(55L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Performer not found: 55");

        verify(performerRepository).existsById(eq(55L));
        verifyNoMoreInteractions(performerRepository);
    }

    @Test
    void delete_shouldCallDeleteById_whenExists() {
        when(performerRepository.existsById(eq(55L))).thenReturn(true);
        service.delete(55L);

        verify(performerRepository).existsById(eq(55L));
        verify(performerRepository).deleteById(eq(55L));
        verifyNoMoreInteractions(performerRepository);
    }
}
