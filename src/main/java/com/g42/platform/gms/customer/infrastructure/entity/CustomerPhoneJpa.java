package com.g42.platform.gms.customer.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Số điện thoại PHỤ của khách (changeset 037). Số chính vẫn nằm ở customer_profile.phone —
 * đó là số đăng nhập và số nhận Zalo. Một số chỉ thuộc về một khách, tính trên cả hai bảng.
 */
@Entity
@Table(name = "customer_phone")
@Getter
@Setter
public class CustomerPhoneJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_phone_id")
    private Integer customerPhoneId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "note", length = 100)
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
