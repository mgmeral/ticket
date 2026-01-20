package dev.mgmeral.ticket.repository;

import dev.mgmeral.ticket.entity.Hold;
import dev.mgmeral.ticket.enums.HoldStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface HoldRepository extends JpaRepository<Hold, Long> {
    Optional<Hold> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select coalesce(sum(h.quantity), 0)
            from Hold h
            where h.seanceId = :seanceId
              and h.status = :status
              and h.expiresAt > :now
            """)
    long sumActiveQuantity(Long seanceId, HoldStatus status, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Hold> findWithLockById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Hold h
               set h.status = :expired,
                   h.releasedAt = :now
             where h.status = :held
               and h.expiresAt <= :now
               and h.releasedAt is null
            """)
    int expireAll(@Param("held") HoldStatus held,
                  @Param("expired") HoldStatus expired,
                  @Param("now") Instant now);

}
