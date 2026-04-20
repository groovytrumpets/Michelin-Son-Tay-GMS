package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ServiceRuleJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRuleJpaRepo extends JpaRepository<ServiceRuleJpa, Integer> {

    List<ServiceRuleJpa> findAllByIsActiveTrue();
}
