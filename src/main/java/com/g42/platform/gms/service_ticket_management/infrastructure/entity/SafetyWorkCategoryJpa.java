package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * Đầu mục kiểm tra an toàn của phiếu dịch vụ (bảng work_category).
 *
 * Từ changeset 014 bảng này CHỈ còn giữ đầu mục kiểm tra an toàn. Danh mục của
 * phụ tùng / dịch vụ đã dọn sang bảng item_category, nên không còn cờ is_default
 * để phân biệt hai loại — mọi bản ghi ở đây đều là đầu mục kiểm tra.
 */
@Entity
@Table(name = "work_category")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafetyWorkCategoryJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idwork_category")
    private Integer id;
    
    @Column(name = "category_code")
    private String categoryCode;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(name = "is_active")
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    private Boolean isActive;

}