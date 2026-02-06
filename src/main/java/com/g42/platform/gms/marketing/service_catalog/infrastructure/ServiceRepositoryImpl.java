package com.g42.platform.gms.marketing.service_catalog.infrastructure;

import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceDetailRespond;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import com.g42.platform.gms.marketing.service_catalog.domain.repository.ServiceRepository;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.mapper.ServiceMapper;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.repository.ServiceJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class ServiceRepositoryImpl implements ServiceRepository {
    private final ServiceJpaRepository serviceJpaRepository;
    private final ServiceMapper serviceMapper;

    @Override
    public List<Service> findAllActive() {
        List<ServiceJpaEntity> serviceJpaEntities = serviceJpaRepository.findAllByStatus(ServiceStatus.ACTIVE);
        return serviceMapper.toDomain(serviceJpaEntities);
    }

    @Override
    public Service findServiceDetailById(Long serviceId) {
        ServiceJpaEntity serviceDetailJpa = serviceJpaRepository.searchByServiceId(serviceId);
        return serviceMapper.toDomain(serviceDetailJpa);
    }
}
