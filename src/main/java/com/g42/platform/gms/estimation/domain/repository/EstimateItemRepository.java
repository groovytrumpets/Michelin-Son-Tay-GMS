package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface EstimateItemRepository {

    List<EstimateItem> findByEstimateIds(List<Integer> estimateIds);

    void saveAll(List<EstimateItem> items);

    List<EstimateItem> findByEstimateId(Integer estimateId);

    void delete(EstimateItem estimateItem);

    EstimateItem findByEstimateItemId(Integer estimateItemId);

    EstimateItem save(EstimateItem estimateItem);

    void deleteAll(List<EstimateItem> giftItems);
}
