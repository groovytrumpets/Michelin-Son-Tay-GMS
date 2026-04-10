package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface StockAllocationRepositoryJpa extends JpaRepository<StockAllocationJpa, Integer> {
    @Transactional
    @Modifying
    @Query("""
        update StockAllocationJpa s set s.status='RELEASED' where s.estimateId = :revisedFromId
        """)
    void updateReleasedEstimateById(@Param("revisedFromId")Integer revisedFromId);

    List<StockAllocationJpa> findAllByEstimateId(Integer estimateId);

    StockAllocationJpa getStockAllocationJpaByAllocationId(Integer allocationId);
}
