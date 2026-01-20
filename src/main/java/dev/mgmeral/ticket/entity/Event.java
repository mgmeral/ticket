package dev.mgmeral.ticket.entity;

import dev.mgmeral.ticket.enums.EventType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Event extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    @Column
    @Enumerated(EnumType.STRING)
    private EventType type;
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private String summary;
    @Column(name = "start_date")
    private Instant startDate;
    @Column(name = "end_date")
    private Instant endDate;
    @ManyToMany
    @JoinTable(
            name = "event_performers",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "performer_id")
    )
    @ToString.Exclude
    private Set<Performer> performers = new HashSet<>();
}
