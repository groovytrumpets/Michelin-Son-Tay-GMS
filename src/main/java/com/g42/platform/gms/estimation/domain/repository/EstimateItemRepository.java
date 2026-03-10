package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;

import java.util.List;

public interface EstimateItemRepository {

    List<EstimateItem> findByEstimateIds(List<Integer> estimateIds);

    void saveAll(List<EstimateItem> items);

    List<EstimateItem> findByEstimateId(Integer estimateId);
}
