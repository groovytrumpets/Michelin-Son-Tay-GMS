package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ServiceRuleJpa;

import java.util.List;
import java.util.Optional;

public interface ServiceRuleRepo {

    List<ServiceRuleJpa> findAllActive();

    Optional<ServiceRuleJpa> findById(Integer ruleId);

    ServiceRuleJpa save(ServiceRuleJpa rule);
}
