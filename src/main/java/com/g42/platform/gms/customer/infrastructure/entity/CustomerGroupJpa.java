package com.g42.platform.gms.customer.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Nhóm khách hàng dùng trong Danh bạ đối tác (bảng customer_group).
 */
@Entity
@Table(name = "customer_group")
@Getter
@Setter
public class CustomerGroupJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "code", length = 50, nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
