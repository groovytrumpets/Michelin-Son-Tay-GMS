package com.g42.platform.gms.booking_management.domain.entity;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemJpaEntity {
    private Integer itemId;
    private String itemName;
    private String itemType;
    private Double estimatedPrice;
    private Boolean isActive;
    private Integer warrantyDurationMonths;
    private ServiceJpaEntity serviceService;
}