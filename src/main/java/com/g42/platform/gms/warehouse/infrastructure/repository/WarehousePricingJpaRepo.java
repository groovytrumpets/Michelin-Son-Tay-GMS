package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehousePricingJpaRepo extends JpaRepository<WarehousePricingJpa, Integer> {

    List<WarehousePricingJpa> findByWarehouseIdAndIsActiveTrueOrderByItemId(Integer warehouseId);

        @Query("""
        select p from WarehousePricingJpa p
        where p.warehouseId = :warehouseId
            and (:isActive is null or p.isActive = :isActive)
            and (
                :search is null
                or str(p.itemId) like concat('%', :search, '%')
            )
        """)
        Page<WarehousePricingJpa> search(
                        @Param("warehouseId") Integer warehouseId,
                        @Param("isActive") Boolean isActive,
                        @Param("search") String search,
                        Pageable pageable);

    java.util.Optional<WarehousePricingJpa> findByWarehouseIdAndItemIdAndIsActiveTrue(Integer warehouseId, Integer itemId);
    @Query("""
    select wp from WarehousePricingJpa wp where wp.itemId=:itemId and wp.warehouseId=:warehouseId and wp.isActive=true 
        """)
    WarehousePricingJpa getWarehousePricingJpaByItemIdAndWarehouseId(Integer itemId, Integer warehouseId);
}
