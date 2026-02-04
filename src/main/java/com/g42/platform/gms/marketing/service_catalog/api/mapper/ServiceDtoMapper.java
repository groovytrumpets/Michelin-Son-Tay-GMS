package com.g42.platform.gms.marketing.service_catalog.api.mapper;

import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceDtoMapper {
    ServiceSumaryRespond toDto(Service service);
    List<ServiceSumaryRespond> toDto(List<Service> services);
}
