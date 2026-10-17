package com.codevo.businessos.project;

import org.mapstruct.*;

@Mapper(componentModel="spring")
public interface ProjectMapper {
    default ProjectDtos.View view(Project e,java.util.List<ProjectDtos.CommercialTotal> totals) {
        boolean fromServices=!totals.isEmpty();
        var own=totals.stream().filter(t->t.currency()==e.getCurrency()).findFirst();
        var contract=fromServices?own.map(ProjectDtos.CommercialTotal::contractValue).orElse(java.math.BigDecimal.ZERO):e.getEstimatedPrice();
        var cost=fromServices?own.map(ProjectDtos.CommercialTotal::estimatedCost).orElse(java.math.BigDecimal.ZERO):e.getEstimatedCost();
        boolean financial=com.codevo.businessos.security.Permissions.currentHas("FINANCE_READ");
        return new ProjectDtos.View(e.getId(),e.getClientId(),e.getName(),e.getProjectCode(),e.getDescription(),e.getStatus(),e.getPriority(),e.getStartDate(),e.getDeadline(),e.getCompletedAt(),e.getAssignedUserId(),financial?e.getEstimatedPrice():null,financial?e.getEstimatedCost():null,e.getCurrency(),e.getNotes(),e.getCreatedAt(),e.getUpdatedAt(),e.getVersion(),financial?contract:null,financial?cost:null,fromServices?"SERVICES":"LEGACY_ESTIMATE",financial?java.util.List.copyOf(totals):java.util.List.of());
    }
    @BeanMapping(ignoreByDefault=true)
    @Mapping(target="clientId", source="clientId")
    @Mapping(target="name", source="name")
    @Mapping(target="projectCode", source="projectCode")
    @Mapping(target="description", source="description")
    @Mapping(target="status", source="status")
    @Mapping(target="priority", source="priority")
    @Mapping(target="startDate", source="startDate")
    @Mapping(target="deadline", source="deadline")
    @Mapping(target="completedAt", source="completedAt")
    @Mapping(target="assignedUserId", source="assignedUserId")
    @Mapping(target="estimatedPrice", source="estimatedPrice")
    @Mapping(target="estimatedCost", source="estimatedCost")
    @Mapping(target="currency", source="currency")
    @Mapping(target="notes", source="notes")
    void apply(ProjectDtos.Request request, @MappingTarget Project entity);
}
