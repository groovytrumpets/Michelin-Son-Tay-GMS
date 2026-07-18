package com.g42.platform.gms.customer.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_points_history")
@Getter
@Setter
public class CustomerPointsHistoryJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer historyId;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "points_delta")
    private Integer pointsDelta;

    @Column(name = "reason")
    private String reason;

    @Column(name = "ref_booking_id")
    private Integer refBookingId;

    @Column(name = "amount_spent")
    private Long amountSpent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
