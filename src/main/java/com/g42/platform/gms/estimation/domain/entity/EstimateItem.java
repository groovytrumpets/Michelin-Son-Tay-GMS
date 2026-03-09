package com.g42.platform.gms.estimation.domain.entity;

import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.WarehouseJpa;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateItem {

    private Integer id;
    private Integer estimate;
    private String estimateId;
    private Integer itemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Boolean isOverridden;
    private String overrideReason;
    private Integer warehouseId;
}
