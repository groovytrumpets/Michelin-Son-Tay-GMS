package com.g42.platform.gms.marketing.siteheader.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bố cục thanh đầu trang khách của một vị trí.
 *
 * <p>Cả bố cục nằm trong một chuỗi JSON ({@code configJson}) chứ không tách mỗi
 * widget một dòng: các widget không có bộ thuộc tính chung, và bố cục luôn được
 * đọc/ghi trọn gói. Lý do đầy đủ ghi ở changeset 009-site-header.yaml.
 */
@Getter
@Setter
@Entity
@Table(name = "site_header_config")
public class SiteHeaderConfigJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id", nullable = false)
    private Integer configId;

    @Column(name = "location_code", nullable = false, length = 50)
    private String locationCode;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
