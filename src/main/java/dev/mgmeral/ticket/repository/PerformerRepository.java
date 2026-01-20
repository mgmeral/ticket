package dev.mgmeral.ticket.repository;

import dev.mgmeral.ticket.entity.Performer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformerRepository extends JpaRepository<Performer, Long> {
    boolean existsByNameIgnoreCase(String name);

    Page<Performer> findByNameContainingIgnoreCase(String name, Pageable pageable);

}
