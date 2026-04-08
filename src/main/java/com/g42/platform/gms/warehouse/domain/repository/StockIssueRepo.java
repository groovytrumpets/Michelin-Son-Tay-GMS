package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueJpa;

import java.util.List;
import java.util.Optional;

public interface StockIssueRepo {

    Optional<StockIssueJpa> findById(Integer issueId);

    List<StockIssueJpa> findByWarehouseId(Integer warehouseId);

    StockIssueJpa save(StockIssueJpa issue);

    boolean existsByCode(String issueCode);
}
