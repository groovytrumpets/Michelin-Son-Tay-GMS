package com.g42.platform.gms.warehouse.infrastructure;

import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.warehouse.infrastructure.mapper.*;
import com.g42.platform.gms.warehouse.infrastructure.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class WarehouseRepoImpl implements WarehouseRepo {
    @Autowired
    private WarehouseJpaMapper warehouseJpaMapper;
    @Autowired
    private WarehousePricingJpaMapper warehousePricingJpaMapper;
    @Autowired
    private InventoryJpaMapper inventoryJpaMapper;
    @Autowired
    private InventoryTransactionJpaMapper inventoryTransactionJpaMapper;
    @Autowired
    private StockTransferJpaMapper stockTransferJpaMapper;
    @Autowired
    private WarehouseJpaRepo  warehouseJpaRepo;
    @Autowired
    private WarehousePricingJpaRepo warehousePricingJpaRepo;
    @Autowired
    private InventoryJpaRepo inventoryJpaRepo;
    @Autowired
    private StockTransferJpaRepo stockTransferJpaRepo;
    @Autowired
    private InventoryTransactionJpaRepo inventoryTransactionJpaRepo;
}
