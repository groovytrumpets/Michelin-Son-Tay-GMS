package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerProfileJpaRepo extends JpaRepository<CustomerProfileJpa,Integer> , JpaSpecificationExecutor<CustomerProfileJpa> {
    
}
