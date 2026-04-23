package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.DiscountConfig;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.repository.DiscountConfigRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.DiscountConfigJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.DiscountConfigJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DiscountConfigRepoImpl implements DiscountConfigRepo {

    private final DiscountConfigJpaRepo jpaRepo;

    @Override
    public List<DiscountConfig> findActiveByItemIdAndIssueType(Integer itemId, IssueType issueType) {
        return jpaRepo.findActiveByItemIdAndIssueType(itemId, issueType)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public DiscountConfig save(DiscountConfig config) {
        DiscountConfigJpa saved = jpaRepo.save(toJpa(config));
        return toDomain(saved);
    }

    private DiscountConfig toDomain(DiscountConfigJpa jpa) {
        DiscountConfig domain = new DiscountConfig();
        domain.setConfigId(jpa.getConfigId());
        domain.setItemId(jpa.getItemId());
        domain.setIssueType(jpa.getIssueType());
        domain.setQuantityThreshold(jpa.getQuantityThreshold());
        domain.setDiscountRate(jpa.getDiscountRate());
        domain.setIsActive(jpa.getIsActive());
        domain.setCreatedBy(jpa.getCreatedBy());
        domain.setCreatedAt(jpa.getCreatedAt());
        return domain;
    }

    private DiscountConfigJpa toJpa(DiscountConfig domain) {
        DiscountConfigJpa jpa = new DiscountConfigJpa();
        jpa.setConfigId(domain.getConfigId());
        jpa.setItemId(domain.getItemId());
        jpa.setIssueType(domain.getIssueType());
        jpa.setQuantityThreshold(domain.getQuantityThreshold());
        jpa.setDiscountRate(domain.getDiscountRate());
        jpa.setIsActive(domain.getIsActive());
        jpa.setCreatedBy(domain.getCreatedBy());
        jpa.setCreatedAt(domain.getCreatedAt());
        return jpa;
    }
}
