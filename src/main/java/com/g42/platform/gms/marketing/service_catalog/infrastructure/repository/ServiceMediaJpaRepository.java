package com.g42.platform.gms.marketing.service_catalog.infrastructure.repository;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceMediaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceMediaJpaRepository extends JpaRepository<ServiceMediaJpaEntity, Integer> {
}
