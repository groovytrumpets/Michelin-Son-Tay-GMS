package com.g42.platform.gms.customerimport.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Dòng dịch vụ hoặc phụ tùng trong một lượt cũ, ghi theo nguyên văn sổ Excel.
 * Không nối vào catalog_item vì tên trong sổ viết tự do, gần 400 cách viết khác
 * nhau cho khoảng 600 dòng.
 */
@Entity
@Table(name = "legacy_visit_item")
@Data
public class LegacyVisitItemJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_visit_item_id")
    private Integer legacyVisitItemId;

    @Column(name = "legacy_visit_id", nullable = false)
    private Integer legacyVisitId;

    @Column(name = "line_no")
    private Short lineNo;

    /**
     * Hạng mục gốc trong sổ (Lốp, Dầu động cơ, Phanh...). Đây là trường để sau này
     * nhắc bảo dưỡng theo loại dịch vụ, và cũng là tên thay thế cho những dòng mà
     * sổ cũ bỏ trống ô Diễn giải.
     */
    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "quantity", precision = 9, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;
}
