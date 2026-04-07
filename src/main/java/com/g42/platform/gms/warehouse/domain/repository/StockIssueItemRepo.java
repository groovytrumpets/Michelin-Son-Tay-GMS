package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueItemJpa;

import java.util.List;

public interface StockIssueItemRepo {

    List<StockIssueItemJpa> saveAll(List<StockIssueItemJpa> items);

    void deleteByIssueId(Integer issueId);
}
