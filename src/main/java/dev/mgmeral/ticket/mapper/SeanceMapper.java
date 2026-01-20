package dev.mgmeral.ticket.mapper;

import dev.mgmeral.ticket.entity.Seance;
import dev.mgmeral.ticket.model.SeanceCreateRequest;
import dev.mgmeral.ticket.model.SeanceCreateResponse;
import dev.mgmeral.ticket.model.SeanceGetResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SeanceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "startDate", source = "request.startDateTime")
    Seance toEntity(Long eventId, SeanceCreateRequest request);

    @Mapping(target = "startDateTime", source = "startDate")
    SeanceCreateResponse toCreateResponse(Seance seance);

    @Mapping(target = "startDateTime", source = "startDate")
    SeanceGetResponse toGetResponse(Seance seance);
}
