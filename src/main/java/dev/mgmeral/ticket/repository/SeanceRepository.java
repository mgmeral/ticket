package dev.mgmeral.ticket.repository;

import dev.mgmeral.ticket.entity.Seance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface SeanceRepository extends JpaRepository<Seance, Long>, JpaSpecificationExecutor<Seance> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Seance> findWithLockById(Long id);
}
