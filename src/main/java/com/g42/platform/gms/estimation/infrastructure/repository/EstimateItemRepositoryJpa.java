package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstimateItemRepositoryJpa extends JpaRepository<EstimateItemJpa, Integer> {
    @Query("SELECT ei FROM EstimateItemJpa ei " +
            "WHERE ei.estimateId IN :estimateIds")
    List<EstimateItemJpa> findByEstimateIds(@Param("estimateIds")List<Integer> estimateIds);

    List<EstimateItemJpa> findByEstimateId(Integer estimateId);

    EstimateItemJpa getEstimateItemJpaById(Integer id);

    EstimateItemJpa findByRevisedFromItemId(Integer revisedFromItemId);
}
