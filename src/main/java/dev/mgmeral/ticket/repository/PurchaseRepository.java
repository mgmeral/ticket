package dev.mgmeral.ticket.repository;

import dev.mgmeral.ticket.entity.Purchase;
import dev.mgmeral.ticket.enums.PurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findByIdempotencyKey(String idempotencyKey);

    boolean existsByPaymentRef(String paymentRef);

    @Query("""
            select coalesce(sum(p.quantity), 0)
            from Purchase p
            where p.seanceId = :seanceId
              and p.status = :status
            """)
    long sumQuantityBySeanceAndStatus(Long seanceId, PurchaseStatus status);

    Optional<Purchase> findByPaymentRef(String paymentRef);
}
