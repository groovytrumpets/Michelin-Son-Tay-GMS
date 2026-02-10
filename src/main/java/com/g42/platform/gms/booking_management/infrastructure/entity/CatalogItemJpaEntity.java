package com.g42.platform.gms.booking_management.infrastructure.entity;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.util.List;

@Entity
@Table(name = "catalog_item")
@Data
public class CatalogItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String itemType; // SERVICE hoặc PART

    private Double estimatedPrice;

    private Boolean isActive = true;
    @ColumnDefault("0")
    @Column(name = "warranty_duration_months")
    private Integer warrantyDurationMonths;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_service_id", nullable = false)
    private ServiceJpaEntity serviceService;
}