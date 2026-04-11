package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.ServiceRuleRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.ServiceRuleJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.ServiceRuleJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ServiceRuleRepoImpl implements ServiceRuleRepo {

    private final ServiceRuleJpaRepo jpaRepo;

    @Override
    public List<ServiceRuleJpa> findAllActive() {
        return jpaRepo.findAllByIsActiveTrue();
    }

    @Override
    public Optional<ServiceRuleJpa> findById(Integer ruleId) {
        return jpaRepo.findById(ruleId);
    }

    @Override
    public ServiceRuleJpa save(ServiceRuleJpa rule) {
        return jpaRepo.save(rule);
    }
}
