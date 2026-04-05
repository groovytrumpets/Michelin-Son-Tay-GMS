package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_issue_item")
@Data
public class StockIssueItemJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_item_id")
    private Integer issueItemId;

    @Column(name = "issue_id", nullable = false)
    private Integer issueId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "export_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal exportPrice;

    @Column(name = "import_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal importPrice;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice;

    // gross_profit là GENERATED ALWAYS AS column trong MySQL — chỉ đọc, không insert/update
    @Column(name = "gross_profit", insertable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal grossProfit;
}
