package com.g42.platform.gms.customer.infrastructure.repository;

import com.g42.platform.gms.customer.infrastructure.entity.CustomerAuthJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAuthJpaRepo extends JpaRepository<CustomerAuthJpa,Integer> {
}
