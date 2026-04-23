package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.ServiceRule;

import java.util.List;
import java.util.Optional;

public interface ServiceRuleRepo {

    List<ServiceRule> findAllActive();

    Optional<ServiceRule> findById(Integer ruleId);

    ServiceRule save(ServiceRule rule);
}
