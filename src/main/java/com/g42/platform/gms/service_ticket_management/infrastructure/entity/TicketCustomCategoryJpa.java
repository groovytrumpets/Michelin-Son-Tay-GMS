package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Bảng lưu hạng mục kiểm tra tùy chỉnh gắn với từng phiếu kiểm tra an toàn.
 * Tách biệt hoàn toàn với work_category (chỉ giữ 13 default).
 */
@Entity
@Table(name = "ticket_custom_category")
@Data
public class TicketCustomCategoryJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "inspection_id", nullable = false)
    private Integer inspectionId;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";
}
