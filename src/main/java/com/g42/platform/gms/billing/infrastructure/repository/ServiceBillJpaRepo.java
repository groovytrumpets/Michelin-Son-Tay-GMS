package com.g42.platform.gms.billing.infrastructure.repository;

import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceBillJpaRepo extends JpaRepository<ServiceBillJpa,Integer> {
    ServiceBillJpa findByEstimateId(Integer estimateId);
}
