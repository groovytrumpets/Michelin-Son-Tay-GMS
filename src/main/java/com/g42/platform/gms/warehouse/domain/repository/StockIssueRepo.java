package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.StockIssue;

import java.util.List;
import java.util.Optional;

public interface StockIssueRepo {
    Optional<StockIssue> findById(Integer issueId);
    List<StockIssue> findByWarehouseId(Integer warehouseId);
    StockIssue save(StockIssue issue);
    boolean existsByCode(String issueCode);
}
