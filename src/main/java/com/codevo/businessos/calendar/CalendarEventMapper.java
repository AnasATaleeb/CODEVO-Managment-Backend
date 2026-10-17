package com.codevo.businessos.calendar;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface CalendarEventMapper {
    CalendarEventDtos.View view(CalendarEvent entity);
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="clientId", source="clientId")
    @Mapping(target="projectId", source="projectId")
    @Mapping(target="title", source="title")
    @Mapping(target="type", source="type")
    @Mapping(target="startAt", source="startAt")
    @Mapping(target="endAt", source="endAt")
    @Mapping(target="assignedUserId", source="assignedUserId")
    @Mapping(target="notes", source="notes")
    void apply(CalendarEventDtos.Request request, @MappingTarget CalendarEvent entity);
}
