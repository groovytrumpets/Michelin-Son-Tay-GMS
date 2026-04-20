package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockAllocationJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockAllocationJpaRepo extends JpaRepository<StockAllocationJpa, Integer> {

    List<StockAllocationJpa> findByServiceTicketIdAndStatus(Integer serviceTicketId, AllocationStatus status);

    List<StockAllocationJpa> findByServiceTicketId(Integer serviceTicketId);

    List<StockAllocationJpa> findByServiceTicketIdAndWarehouseIdAndStatus(Integer serviceTicketId, Integer warehouseId, AllocationStatus status);

    List<StockAllocationJpa> findByIssueIdAndStatus(Integer issueId, AllocationStatus status);

    List<StockAllocationJpa> findByEstimateItemId(Integer estimateItemId);

    @Query("SELECT SUM(a.quantity) FROM StockAllocationJpa a WHERE a.warehouseId = :warehouseId AND a.itemId = :itemId AND a.status = 'RESERVED'")
    Integer sumReservedQuantity(@Param("warehouseId") Integer warehouseId, @Param("itemId") Integer itemId);
}
