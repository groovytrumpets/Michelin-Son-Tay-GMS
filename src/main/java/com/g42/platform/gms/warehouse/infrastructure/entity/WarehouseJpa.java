package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.common.enums.WarehouseTypeEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "warehouse", schema = "michelin_garage")
public class WarehouseJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_id", nullable = false)
    private Integer warehouseId;

    @Size(max = 20)
    @NotNull
    @Column(name = "warehouse_code", nullable = false, length = 20)
    private String warehouseCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "warehouse_name", nullable = false, length = 100)
    private String warehouseName;
    @Enumerated(EnumType.STRING)
    @NotNull
    @Lob
    @Column(name = "warehouse_type", nullable = false)
    private WarehouseTypeEnum warehouseType;

    @Column(name = "parent_warehouse_id", nullable = false)
    private Integer parentWarehouseId;

    @Lob
    @Column(name = "address")
    private String address;

    @Column(name = "manager_staff_id")
    private Integer managerStaffId;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;


}