package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.repository.WarehouseDetailProjection;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface WarehouseJpaRepo extends JpaRepository<WarehouseJpa,Integer> {
//    @Query(value = """
//    SELECT
//        w.warehouse_id AS warehouseId,
//        w.warehouse_code AS warehouseCode,
//        w.warehouse_name AS warehouseName,
//        wp.item_id AS itemId,
//        wp.base_price AS basePrice,
//        wp.markup_multiplier AS markupMultiplier,
//        wp.selling_price AS sellingPrice,
//        i.quantity AS quantity,
//        i.reserved_quantity AS reservedQuantity,
//        i.import_price AS importPrice,
//        i.min_stock_level AS minStockLevel,
//        i.max_stock_level AS maxStockLevel
//    FROM warehouse_pricing wp
//    JOIN warehouse w ON wp.warehouse_id = w.warehouse_id
//    LEFT JOIN inventory i ON wp.warehouse_id = i.warehouse_id AND wp.item_id = i.item_id
//    WHERE wp.item_id = :itemId
//    AND wp.is_active = 1
//    """, nativeQuery = true)
    @Query("""
    select w.warehouseId as  warehouseId,
        w.warehouseCode as warehouseCode,
            w.warehouseName as warehouseName,
                w.address as warehouseAddress,
        wp.itemId as itemId,
        wp.sellingPrice as sellingPrice,
                    i.quantity as quantity,
                        i.reservedQuantity as reservedQuantity,
                            i.minStockLevel as minStockLevel,
                                i.maxStockLevel as maxStockLevel
             from WarehousePricingJpa wp join WarehouseJpa w on wp.warehouseId = w.warehouseId
        left join InventoryJpa i on wp.warehouseId = i.warehouseId and wp.itemId = i.itemId
            where wp.itemId in(:itemId) and wp.isActive= true
        """)
    List<WarehouseDetailProjection> getListOfWarehouseDetailsByItemIds(@Param("itemId")Set<Integer> itemIds);
}
