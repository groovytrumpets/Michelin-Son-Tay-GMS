package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerProfileJpaRepo extends JpaRepository<CustomerProfileJpa,Integer> {
    
}
