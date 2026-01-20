package dev.mgmeral.ticket.repository.spec;

import dev.mgmeral.ticket.entity.Seance;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class SeanceSpecifications {

    private SeanceSpecifications() {
    }

    public static Specification<Seance> eventIdEquals(Long eventId) {
        return (root, query, cb) -> cb.equal(root.get("eventId"), eventId);
    }

    public static Specification<Seance> startDateGte(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), from);
    }

    public static Specification<Seance> startDateLte(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), to);
    }
}
