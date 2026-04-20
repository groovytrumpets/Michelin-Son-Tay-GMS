package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

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
    public List<DiscountConfigJpa> findActiveByItemIdAndIssueType(Integer itemId, IssueType issueType) {
        return jpaRepo.findActiveByItemIdAndIssueType(itemId, issueType);
    }

    @Override
    public DiscountConfigJpa save(DiscountConfigJpa config) {
        return jpaRepo.save(config);
    }
}
