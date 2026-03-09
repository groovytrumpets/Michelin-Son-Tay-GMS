package com.g42.platform.gms.estimation.infrastructure.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
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
    private Integer id;

    @Size(max = 20)
    @NotNull
    @Column(name = "warehouse_code", nullable = false, length = 20)
    private String warehouseCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "warehouse_name", nullable = false, length = 100)
    private String warehouseName;

    @NotNull
    @Lob
    @Column(name = "warehouse_type", nullable = false)
    private String warehouseType;

    @Column(name = "parent_warehouse_id", nullable = false)
    private Integer parentWarehouseId;

    @Lob
    @Column(name = "address")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_staff_id")
    private StaffProfile managerStaff;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;


}