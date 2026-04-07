package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.infrastructure.entity.DiscountConfigJpa;

import java.util.List;

public interface DiscountConfigRepo {

    List<DiscountConfigJpa> findActiveByItemIdAndIssueType(Integer itemId, IssueType issueType);

    DiscountConfigJpa save(DiscountConfigJpa config);
}
