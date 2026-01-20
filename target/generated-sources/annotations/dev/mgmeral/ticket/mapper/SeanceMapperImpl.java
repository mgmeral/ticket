package dev.mgmeral.ticket.mapper;

import dev.mgmeral.ticket.entity.Seance;
import dev.mgmeral.ticket.model.SeanceCreateRequest;
import dev.mgmeral.ticket.model.SeanceCreateResponse;
import dev.mgmeral.ticket.model.SeanceGetResponse;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-20T03:23:42+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21 (Oracle Corporation)"
)
@Component
public class SeanceMapperImpl implements SeanceMapper {

    @Override
    public Seance toEntity(Long eventId, SeanceCreateRequest request) {
        if ( eventId == null && request == null ) {
            return null;
        }

        Seance seance = new Seance();

        if ( request != null ) {
            seance.setStartDate( request.startDateTime() );
            seance.setCapacity( request.capacity() );
        }
        seance.setEventId( eventId );

        return seance;
    }

    @Override
    public SeanceCreateResponse toCreateResponse(Seance seance) {
        if ( seance == null ) {
            return null;
        }

        Instant startDateTime = null;
        Long id = null;
        Long eventId = null;
        int capacity = 0;

        startDateTime = seance.getStartDate();
        id = seance.getId();
        eventId = seance.getEventId();
        capacity = seance.getCapacity();

        SeanceCreateResponse seanceCreateResponse = new SeanceCreateResponse( id, eventId, startDateTime, capacity );

        return seanceCreateResponse;
    }

    @Override
    public SeanceGetResponse toGetResponse(Seance seance) {
        if ( seance == null ) {
            return null;
        }

        Instant startDateTime = null;
        Long id = null;
        Long eventId = null;
        int capacity = 0;

        startDateTime = seance.getStartDate();
        id = seance.getId();
        eventId = seance.getEventId();
        capacity = seance.getCapacity();

        SeanceGetResponse seanceGetResponse = new SeanceGetResponse( id, eventId, startDateTime, capacity );

        return seanceGetResponse;
    }
}
