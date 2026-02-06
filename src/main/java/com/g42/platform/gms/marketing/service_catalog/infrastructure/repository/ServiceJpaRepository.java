package com.g42.platform.gms.marketing.service_catalog.infrastructure.repository;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceJpaRepository extends JpaRepository<ServiceJpaEntity, Long> {

    List<ServiceJpaEntity> findAllByStatus(ServiceStatus status);

    ServiceJpaEntity searchByServiceId(Long serviceId);
}
