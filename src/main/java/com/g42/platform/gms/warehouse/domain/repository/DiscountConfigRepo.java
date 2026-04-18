package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.DiscountConfig;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;

import java.util.List;

public interface DiscountConfigRepo {

    List<DiscountConfig> findActiveByItemIdAndIssueType(Integer itemId, IssueType issueType);

    DiscountConfig save(DiscountConfig config);
}
