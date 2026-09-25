package com.g42.platform.gms.branch.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một xưởng dùng chung hệ thống (Liquibase 044).
 *
 * Chỉ để GHI NHẬN lịch hẹn / phiếu phát sinh ở đâu — kho và nhân viên dùng chung giữa
 * các xưởng nên không có gì khác gắn vào đây. Luôn có đúng một xưởng {@code isDefault}:
 * máy chưa chọn xưởng và khách đặt lịch online không chọn xưởng thì rơi về xưởng đó.
 */
@Entity
@Table(name = "branch")
@Getter
@Setter
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "branch_code", nullable = false, length = 20, unique = true)
    private String branchCode;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
