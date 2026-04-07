package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.StockIssueItemRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockIssueItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockIssueItemRepoImpl implements StockIssueItemRepo {

    private final StockIssueItemJpaRepo jpaRepo;

    @Override
    public List<StockIssueItemJpa> saveAll(List<StockIssueItemJpa> items) {
        return jpaRepo.saveAll(items);
    }

    @Override
    public void deleteByIssueId(Integer issueId) {
        jpaRepo.deleteByIssueId(issueId);
    }
}
