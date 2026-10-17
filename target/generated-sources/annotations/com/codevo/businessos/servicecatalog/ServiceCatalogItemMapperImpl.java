package com.codevo.businessos.servicecatalog;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-17T20:33:30+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 26.0.1 (Homebrew)"
)
@Component
public class ServiceCatalogItemMapperImpl implements ServiceCatalogItemMapper {

    @Override
    public void apply(ServiceCatalogItemDtos.Request request, ServiceCatalogItem entity) {
        if ( request == null ) {
            return;
        }

        entity.setName( request.name() );
        entity.setCode( request.code() );
        entity.setCategory( request.category() );
        entity.setDescription( request.description() );
        entity.setDefaultBillingType( request.defaultBillingType() );
        entity.setDefaultPrice( request.defaultPrice() );
        entity.setDefaultCost( request.defaultCost() );
        entity.setDefaultCurrency( request.defaultCurrency() );
        entity.setActive( request.active() );
    }
}
