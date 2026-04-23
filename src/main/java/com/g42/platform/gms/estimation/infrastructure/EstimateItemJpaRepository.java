package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

interface EstimateItemJpaRepository extends JpaRepository<EstimateItemJpa, Integer> {
    List<EstimateItemJpa> findByEstimateId(Integer estimateId);

    List<EstimateItemJpa> findByEstimateIdAndIsRemoved(Integer estimateId, Boolean isRemoved);
    @Query("""
        select ei from EstimateItemJpa ei,EstimateJpa e where ei.estimateId=e.id 
                and e.serviceTicketId=:serviceTicketId
        """)
    List<EstimateItemJpa> findByServiceTicketId(Integer serviceTicketId);
}
