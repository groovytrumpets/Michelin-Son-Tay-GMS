package com.g42.platform.gms.customercare.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Một cuộc gọi chăm sóc khách. Changeset 045-1. */
@Entity
@Table(name = "customer_care_call")
@Data
public class CustomerCareCallJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_call_id")
    private Integer careCallId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "called_at", nullable = false)
    private LocalDateTime calledAt;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "reached", nullable = false)
    private Boolean reached = false;

    /** Mã CareCallOutcome. Để String thay vì enum để mã cũ không làm vỡ việc đọc. */
    @Column(name = "outcome", length = 30, nullable = false)
    private String outcome;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
