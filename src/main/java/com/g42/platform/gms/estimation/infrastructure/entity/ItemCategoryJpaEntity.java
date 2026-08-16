package com.g42.platform.gms.estimation.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * Bảng item_category nhìn từ module báo giá — chỉ đọc để nhóm dòng và tra thuế.
 * Việc thêm/sửa/xóa danh mục do module warehouse đảm nhiệm.
 */
@Getter
@Setter
@Entity(name = "EstimateItemCategory")
@Table(name = "item_category")
public class ItemCategoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_category_id", nullable = false)
    private Integer id;

    @Size(max = 50)
    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Size(max = 20)
    @Column(name = "category_type", length = 20)
    private String categoryType;

    @ColumnDefault("0")
    @Column(name = "display_order")
    private Integer displayOrder;

    @ColumnDefault("1")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "tax_rule_id")
    private Integer taxRuleId;
}
