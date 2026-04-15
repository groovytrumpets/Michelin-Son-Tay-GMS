package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface EstimateItemJpaRepository extends JpaRepository<EstimateItemJpa, Integer> {
    List<EstimateItem> findByEstimateId(Integer estimateId);

    List<EstimateItemJpa> findByEstimateIdAndIsRemoved(Integer estimateId, Boolean isRemoved);
}
