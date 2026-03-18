package com.g42.platform.gms.estimation.domain.entity;

import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.domain.enums.EstimateTypeEnum;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estimate {

    private Integer id;
    private Integer serviceTicketId;
    private EstimateTypeEnum estimateType;
    private EstimateEnum status;
    private Instant createdAt;
    private Instant approvedAt;
    private Integer version;
    private Integer revisedFromId;
    private BigDecimal totalPrice;
    private List<EstimateItem> items;

//    public BigDecimal getTotalPrices() {
//        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
//        return items.stream()
//                .map(EstimateItem::getSubTotal)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
}
