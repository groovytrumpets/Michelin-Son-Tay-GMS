package com.g42.platform.gms.marketing.service_catalog.api.mapper;

import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceCreateRequest;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceDetailRespond;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.CatalogItem;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.ServiceMedia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceDtoMapper {
    @Mapping(source = "catalogItems", target = "catalogItemId", qualifiedByName = "mapCatalogId")
    ServiceSumaryRespond toDto(Service service);

    @Named("mapCatalogId")
    default int mapCatalogId(List<CatalogItem> items) {
        if (items == null || items.isEmpty()) return -1;
        return items.get(0).getItemId();
    }
    List<ServiceSumaryRespond> toDto(List<Service> services);
    ServiceDetailRespond toDetailDto(Service service);
    @Mapping(target = "mediaThumbnail", ignore = true)
    @Mapping(target = "media", ignore = true)
    Service toEntity(ServiceCreateRequest request);
}
