package com.g42.platform.gms.customercare.infrastructure.repository;

import com.g42.platform.gms.customercare.infrastructure.entity.CustomerCareProfileJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerCareProfileRepository extends JpaRepository<CustomerCareProfileJpa, Integer> {
}
