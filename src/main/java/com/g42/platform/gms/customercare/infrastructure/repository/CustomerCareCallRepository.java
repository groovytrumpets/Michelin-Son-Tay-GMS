package com.g42.platform.gms.customercare.infrastructure.repository;

import com.g42.platform.gms.customercare.infrastructure.entity.CustomerCareCallJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCareCallRepository extends JpaRepository<CustomerCareCallJpa, Integer> {
}
