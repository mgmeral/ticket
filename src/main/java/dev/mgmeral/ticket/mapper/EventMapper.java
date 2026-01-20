package dev.mgmeral.ticket.mapper;

import dev.mgmeral.ticket.entity.Event;
import dev.mgmeral.ticket.model.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "performers", ignore = true)
    Event toEntity(EventCreateRequest request);

    EventCreateResponse toCreateResponse(Event event);

    EventGetResponse toGetResponse(Event event);

    EventSearchResponse toSearchResponse(Event event);

    EventUpdateResponse toUpdateResponse(Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "performers", ignore = true)
    void updateEntity(@MappingTarget Event event, EventUpdateRequest request);
}
