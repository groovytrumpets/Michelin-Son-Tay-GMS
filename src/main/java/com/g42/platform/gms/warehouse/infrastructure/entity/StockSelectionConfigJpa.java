package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.warehouse.domain.enums.StockAllocationMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * Cấu hình chọn kho / lô tự động khi thêm vật tư vào bảng báo giá.
 * Hệ thống chỉ dùng dòng is_active mới nhất.
 */
@Entity
@Table(name = "stock_selection_config")
@Getter
@Setter
public class StockSelectionConfigJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id", nullable = false)
    private Integer configId;

    @Column(name = "default_warehouse_id")
    private Integer defaultWarehouseId;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'FIFO'")
    @Column(name = "allocation_method", nullable = false, length = 20)
    private StockAllocationMethod allocationMethod = StockAllocationMethod.FIFO;

    @ColumnDefault("1")
    @Column(name = "fallback_to_any_warehouse")
    private Boolean fallbackToAnyWarehouse = true;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;
}
