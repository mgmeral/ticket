package dev.mgmeral.ticket.repository.spec;

import dev.mgmeral.ticket.entity.Event;
import dev.mgmeral.ticket.enums.EventType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class EventSpecifications {
    private EventSpecifications() {
    }

    public static Specification<Event> typeEquals(EventType type) {
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Event> nameContains(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Event> startDateGte(Instant startFrom) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), startFrom);
    }

    public static Specification<Event> startDateLte(Instant startTo) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), startTo);
    }
}
