package com.codevo.businessos.task;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface TaskMapper {
    TaskDtos.View view(Task entity);
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="projectId", source="projectId")
    @Mapping(target="clientId", source="clientId")
    @Mapping(target="title", source="title")
    @Mapping(target="description", source="description")
    @Mapping(target="status", source="status")
    @Mapping(target="priority", source="priority")
    @Mapping(target="assignedUserId", source="assignedUserId")
    @Mapping(target="dueAt", source="dueAt")
    @Mapping(target="completedAt", source="completedAt")
    @Mapping(target="sortOrder", source="sortOrder")
    void apply(TaskDtos.Request request, @MappingTarget Task entity);
}
