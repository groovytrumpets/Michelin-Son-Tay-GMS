package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.infrastructure.entity.CommissionAllocationJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommissionAllocationJpaRepo extends JpaRepository<CommissionAllocationJpa, Integer> {

    List<CommissionAllocationJpa> findByEstimateId(Integer estimateId);

    List<CommissionAllocationJpa> findByEstimateIdIn(List<Integer> estimateIds);

    void deleteByEstimateId(Integer estimateId);
}
