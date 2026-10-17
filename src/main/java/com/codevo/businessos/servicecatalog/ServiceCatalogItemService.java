package com.codevo.businessos.servicecatalog;

import com.codevo.businessos.common.*;
import com.codevo.businessos.common.Currency;
import java.util.UUID;
import java.time.*;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.codevo.businessos.activity.ActivityService;

@Service
@Transactional(readOnly=true)
public class ServiceCatalogItemService {
    private final ServiceCatalogItemRepository repo;
    private final ServiceCatalogItemMapper mapper;
    private final ReferenceChecks refs;
    private final ActivityService activity;
    private final Clock clock;
    public ServiceCatalogItemService(ServiceCatalogItemRepository repo, ServiceCatalogItemMapper mapper, ReferenceChecks refs, ActivityService activity, Clock clock) {
        this.repo=repo; this.mapper=mapper; this.refs=refs; this.activity=activity; this.clock=clock;
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public PageResponse<ServiceCatalogItemDtos.View> list(ListFilter filter) {
        return PageResponse.of(repo.findAll(filter.specification("name",null,Set.of(),null),filter.pageable(Set.of("createdAt","name"))).map(mapper::view));
    }
    @PreAuthorize("hasAuthority('WORK_READ')")
    public ServiceCatalogItemDtos.View get(UUID id) { return mapper.view(repo.findById(id).orElseThrow(()->DomainException.missing("ServiceCatalogItem"))); }
    @Transactional @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    public ServiceCatalogItemDtos.View create(ServiceCatalogItemDtos.Request request) {
        var e=new ServiceCatalogItem(); mapper.apply(request,e); validate(e); repo.saveAndFlush(e);
        activity.record("service_catalog",e.getId(),null,"CREATED","Created ServiceCatalogItem: "+e.getName());
        return mapper.view(e);
    }
    @Transactional @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    public ServiceCatalogItemDtos.View update(UUID id, ServiceCatalogItemDtos.Request request) {
        var e=repo.lockById(id).orElseThrow(()->DomainException.missing("ServiceCatalogItem"));
        if(request.version()==null || request.version()!=e.getVersion()) throw DomainException.conflict("Record changed; refresh before saving");
        
        mapper.apply(request,e); validate(e); repo.flush();
        activity.record("service_catalog",e.getId(),null,"UPDATED","Updated ServiceCatalogItem: "+e.getName());
        return mapper.view(e);
    }
    private void validate(ServiceCatalogItem e) {
        e.setDefaultPrice(Money.amount(e.getDefaultPrice(),e.getDefaultCurrency())); e.setDefaultCost(Money.amount(e.getDefaultCost(),e.getDefaultCurrency()));
    }
}
