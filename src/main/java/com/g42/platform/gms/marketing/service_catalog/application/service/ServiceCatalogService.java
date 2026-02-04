package com.g42.platform.gms.marketing.service_catalog.application.service;

import com.g42.platform.gms.auth.mapper.StaffAuthMapper;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.api.mapper.ServiceDtoMapper;
import com.g42.platform.gms.marketing.service_catalog.domain.repository.ServiceRepository;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.mapper.ServiceMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final ServiceDtoMapper serviceDtoMapper;

    public List<ServiceSumaryRespond> getListActiveServices() {
        var service = serviceRepository.findAllActive();
        return serviceDtoMapper.toDto(service);
    }
}
