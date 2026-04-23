package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.warehouse.domain.entity.ServiceRule;
import com.g42.platform.gms.warehouse.domain.repository.ServiceRuleRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.ServiceRuleJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.ServiceRuleJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ServiceRuleRepoImpl implements ServiceRuleRepo {

    private final ServiceRuleJpaRepo jpaRepo;
    private final ObjectMapper objectMapper;

    @Override
    public List<ServiceRule> findAllActive() {
        return jpaRepo.findAllByIsActiveTrue().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ServiceRule> findById(Integer ruleId) {
        return jpaRepo.findById(ruleId).map(this::toDomain);
    }

    @Override
    public ServiceRule save(ServiceRule rule) {
        ServiceRuleJpa saved = jpaRepo.save(toJpa(rule));
        return toDomain(saved);
    }

    private ServiceRule toDomain(ServiceRuleJpa jpa) {
        ServiceRule domain = new ServiceRule();
        domain.setRuleId(jpa.getRuleId());
        domain.setVehicleTypePattern(jpa.getVehicleTypePattern());
        domain.setKmThreshold(jpa.getKmThreshold());
        domain.setSuggestedItemIds(fromJson(jpa.getSuggestedItemIds()));
        domain.setReason(jpa.getReason());
        domain.setIsActive(jpa.getIsActive());
        domain.setCreatedBy(jpa.getCreatedBy());
        domain.setCreatedAt(jpa.getCreatedAt());
        domain.setUpdatedAt(jpa.getUpdatedAt());
        return domain;
    }

    private ServiceRuleJpa toJpa(ServiceRule domain) {
        ServiceRuleJpa jpa = new ServiceRuleJpa();
        jpa.setRuleId(domain.getRuleId());
        jpa.setVehicleTypePattern(domain.getVehicleTypePattern());
        jpa.setKmThreshold(domain.getKmThreshold());
        jpa.setSuggestedItemIds(toJson(domain.getSuggestedItemIds()));
        jpa.setReason(domain.getReason());
        jpa.setIsActive(domain.getIsActive());
        jpa.setCreatedBy(domain.getCreatedBy());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        return jpa;
    }

    private String toJson(List<Integer> ids) {
        if (ids == null) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<Integer> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
