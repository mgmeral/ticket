package dev.mgmeral.ticket.entity;

import dev.mgmeral.ticket.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_ref", columnNames = "payment_ref")
        }
)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_ref", nullable = false, length = 64)
    private String paymentRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
}
