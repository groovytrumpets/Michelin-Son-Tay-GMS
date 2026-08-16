package com.g42.platform.gms.service_ticket_management.domain.entity;

import lombok.Data;

/**
 * Một đầu mục trong danh sách kiểm tra an toàn của phiếu dịch vụ.
 *
 * Không còn cờ isDefault: từ changeset 014 bảng work_category chỉ chứa đầu mục
 * kiểm tra an toàn, danh mục phụ tùng/dịch vụ đã tách sang bảng item_category.
 */
@Data
public class WorkCategory {
    private Integer id;
    private String categoryCode;
    private String categoryName;
    private Integer displayOrder;
    private Boolean isActive;

    public void initializeDefaults() {
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
}
