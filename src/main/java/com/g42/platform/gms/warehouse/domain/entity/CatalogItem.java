package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItem {
    private Integer itemId;
    private String itemName;
    private CatalogItemType itemType;
    private Double estimatedPrice;
    private Boolean isActive = true;
    private Integer warrantyDurationMonths;
    private ServiceJpaEntity serviceService;
    private String sku;
    private Double price;
    private Boolean showPrice;
    private String description;
    private String imageUrl;
    private Integer comboDurationMonths;
    private String comboDescription;
    private Boolean isRecurring;
}