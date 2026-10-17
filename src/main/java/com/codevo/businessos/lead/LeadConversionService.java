package com.codevo.businessos.lead;

import com.codevo.businessos.client.*;
import com.codevo.businessos.project.*;
import com.codevo.businessos.activity.ActivityService;
import com.codevo.businessos.common.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadConversionService {
    public record Request(@Size(max=255) String projectName) {}
    public record Result(UUID clientId,UUID projectId) {}
    private final LeadRepository leads;private final ClientService clients;private final ProjectApplicationService projects;private final ActivityService activity;private final Clock clock; private final BusinessDates dates;
    public LeadConversionService(LeadRepository leads,ClientService clients,ProjectApplicationService projects,ActivityService activity,Clock clock, BusinessDates dates) {
        this.leads=leads;this.clients=clients;this.projects=projects;this.activity=activity;this.clock=clock; this.dates=dates;
    }
    @Transactional @PreAuthorize("hasAuthority('CRM_WRITE')")
    public Result convert(UUID id,Request request) {
        var lead=leads.lockById(id).orElseThrow(()->DomainException.missing("Lead"));
        if(lead.getConvertedClientId()!=null) return new Result(lead.getConvertedClientId(),null);
        if(!lead.getStage().equals("WON")) throw DomainException.conflict("Mark lead WON before conversion");
        var client=clients.create(new ClientDtos.Request(lead.getName(),lead.getCompanyName(),lead.getName(),lead.getEmail(),
            lead.getPhone(),lead.getWhatsapp(),null,null,null,lead.getSource(),"ACTIVE",lead.getNotes(),lead.getCurrency(),null,
            lead.getAssignedUserId(),dates.today(),dates.today(),null));
        UUID projectId=null;
        if(request.projectName()!=null && !request.projectName().isBlank()) {
            var project=projects.create(new ProjectDtos.Request(client.id(),request.projectName(),"PRJ-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(),
                lead.getServiceInterest(),"PLANNED",lead.getPriority(),dates.today(),null,null,lead.getAssignedUserId(),
                lead.getEstimatedValue(),BigDecimal.ZERO,lead.getCurrency(),lead.getNotes(),null));
            projectId=project.id();
        }
        lead.setConvertedClientId(client.id());leads.flush();
        activity.record("leads",id,client.id(),"CONVERTED","Lead converted to client; original lead history retained");
        return new Result(client.id(),projectId);
    }
}
