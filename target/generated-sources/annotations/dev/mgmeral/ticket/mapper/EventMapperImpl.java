package dev.mgmeral.ticket.mapper;

import dev.mgmeral.ticket.entity.Event;
import dev.mgmeral.ticket.enums.EventType;
import dev.mgmeral.ticket.model.EventCreateRequest;
import dev.mgmeral.ticket.model.EventCreateResponse;
import dev.mgmeral.ticket.model.EventGetResponse;
import dev.mgmeral.ticket.model.EventSearchResponse;
import dev.mgmeral.ticket.model.EventUpdateRequest;
import dev.mgmeral.ticket.model.EventUpdateResponse;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-20T03:23:43+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21 (Oracle Corporation)"
)
@Component
public class EventMapperImpl implements EventMapper {

    @Override
    public Event toEntity(EventCreateRequest request) {
        if ( request == null ) {
            return null;
        }

        Event event = new Event();

        event.setType( request.type() );
        event.setName( request.name() );
        event.setDescription( request.description() );
        event.setSummary( request.summary() );
        event.setStartDate( request.startDate() );
        event.setEndDate( request.endDate() );

        return event;
    }

    @Override
    public EventCreateResponse toCreateResponse(Event event) {
        if ( event == null ) {
            return null;
        }

        Long id = null;
        EventType type = null;
        String name = null;
        String description = null;
        String summary = null;
        Instant startDate = null;
        Instant endDate = null;

        id = event.getId();
        type = event.getType();
        name = event.getName();
        description = event.getDescription();
        summary = event.getSummary();
        startDate = event.getStartDate();
        endDate = event.getEndDate();

        EventCreateResponse eventCreateResponse = new EventCreateResponse( id, type, name, description, summary, startDate, endDate );

        return eventCreateResponse;
    }

    @Override
    public EventGetResponse toGetResponse(Event event) {
        if ( event == null ) {
            return null;
        }

        Long id = null;
        EventType type = null;
        String name = null;
        String description = null;
        String summary = null;
        Instant startDate = null;
        Instant endDate = null;

        id = event.getId();
        type = event.getType();
        name = event.getName();
        description = event.getDescription();
        summary = event.getSummary();
        startDate = event.getStartDate();
        endDate = event.getEndDate();

        EventGetResponse eventGetResponse = new EventGetResponse( id, type, name, description, summary, startDate, endDate );

        return eventGetResponse;
    }

    @Override
    public EventSearchResponse toSearchResponse(Event event) {
        if ( event == null ) {
            return null;
        }

        Long id = null;
        String type = null;
        String name = null;
        Instant startDate = null;
        Instant endDate = null;

        id = event.getId();
        if ( event.getType() != null ) {
            type = event.getType().name();
        }
        name = event.getName();
        startDate = event.getStartDate();
        endDate = event.getEndDate();

        EventSearchResponse eventSearchResponse = new EventSearchResponse( id, type, name, startDate, endDate );

        return eventSearchResponse;
    }

    @Override
    public EventUpdateResponse toUpdateResponse(Event event) {
        if ( event == null ) {
            return null;
        }

        Long id = null;
        EventType type = null;
        String name = null;
        String description = null;
        String summary = null;
        Instant startDate = null;
        Instant endDate = null;

        id = event.getId();
        type = event.getType();
        name = event.getName();
        description = event.getDescription();
        summary = event.getSummary();
        startDate = event.getStartDate();
        endDate = event.getEndDate();

        EventUpdateResponse eventUpdateResponse = new EventUpdateResponse( id, type, name, description, summary, startDate, endDate );

        return eventUpdateResponse;
    }

    @Override
    public void updateEntity(Event event, EventUpdateRequest request) {
        if ( request == null ) {
            return;
        }

        if ( request.type() != null ) {
            event.setType( request.type() );
        }
        if ( request.name() != null ) {
            event.setName( request.name() );
        }
        if ( request.description() != null ) {
            event.setDescription( request.description() );
        }
        if ( request.summary() != null ) {
            event.setSummary( request.summary() );
        }
        if ( request.startDate() != null ) {
            event.setStartDate( request.startDate() );
        }
        if ( request.endDate() != null ) {
            event.setEndDate( request.endDate() );
        }
    }
}
