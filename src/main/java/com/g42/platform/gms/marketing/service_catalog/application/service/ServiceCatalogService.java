package com.g42.platform.gms.marketing.service_catalog.application.service;

import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceDetailRespond;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.api.mapper.ServiceDtoMapper;
import com.g42.platform.gms.marketing.service_catalog.domain.exception.ServiceErrorCode;
import com.g42.platform.gms.marketing.service_catalog.domain.exception.ServiceException;
import com.g42.platform.gms.marketing.service_catalog.domain.repository.ServiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ServiceCatalogService {

    private final ServiceRepository serviceRepository;
    private final ServiceDtoMapper serviceDtoMapper;

    public List<ServiceSumaryRespond> getListActiveServices() {
        LocalDateTime now = LocalDateTime.now();

        return serviceRepository.findAllActive().stream().filter(service -> service.isVisibleNow(now)).map(serviceDtoMapper::toDto).toList();
    }
    @Transactional(noRollbackFor = ServiceException.class)
    public ServiceDetailRespond getServiceDetailById(Long serviceId) {
        com.g42.platform.gms.marketing.service_catalog.domain.entity.Service service =serviceRepository.findServiceDetailById(serviceId);
        if (service == null) {
            throw new ServiceException("Service not found", ServiceErrorCode.SERVICE_NOT_FOUND);
        }
        if (!service.isVisibleNow(LocalDateTime.now())) {
            throw new ServiceException("Service expired", ServiceErrorCode.SERVICE_EXPIRED);
        }
        return serviceDtoMapper.toDetailDto(service);
    }
}
