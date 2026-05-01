package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;

import java.util.List;
import java.util.Optional;

public interface StockIssueItemRepo {
    List<StockIssueItem> saveAll(List<StockIssueItem> items);
    void deleteByIssueId(Integer issueId);
    void deleteById(Integer issueItemId);
    Optional<StockIssueItem> findById(Integer issueItemId);
    StockIssueItem save(StockIssueItem item);
    List<StockIssueItem> findByIssueId(Integer issueId);
}
