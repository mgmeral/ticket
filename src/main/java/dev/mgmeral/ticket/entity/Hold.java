package dev.mgmeral.ticket.entity;

import dev.mgmeral.ticket.enums.HoldStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "holds")
public class Hold extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "seance_id", nullable = false)
    private Long seanceId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;
}
