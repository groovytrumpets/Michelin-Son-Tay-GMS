package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockIssueJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockIssueRepoImpl implements StockIssueRepo {

    private final StockIssueJpaRepo jpaRepo;

    @Override
    public Optional<StockIssueJpa> findById(Integer issueId) {
        return jpaRepo.findById(issueId);
    }

    @Override
    public List<StockIssueJpa> findByWarehouseId(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
    }

    @Override
    public StockIssueJpa save(StockIssueJpa issue) {
        return jpaRepo.save(issue);
    }

    @Override
    public boolean existsByCode(String issueCode) {
        return jpaRepo.existsByIssueCode(issueCode);
    }
}
