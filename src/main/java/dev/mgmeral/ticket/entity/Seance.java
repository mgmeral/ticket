package dev.mgmeral.ticket.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Table(name = "seances")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Seance extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    @Column(name = "event_id")
    private Long eventId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Event event;
    @Column
    private int capacity;
    @Column(name = "start_date")
    private Instant startDate;
}
