package dev.mgmeral.ticket.entity;

import dev.mgmeral.ticket.enums.PurchaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "purchases",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_purchases_idempotency", columnNames = "idempotency_key"),
                @UniqueConstraint(name = "uk_purchases_paymentref", columnNames = "payment_ref")
        },
        indexes = {
                @Index(name = "idx_purchases_seance_status", columnList = "seance_id,status")
        }
)
public class Purchase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_id", nullable = false)
    private Long holdId;

    @Column(name = "seance_id", nullable = false)
    private Long seanceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_ref", nullable = false, length = 64)
    private String paymentRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

}
