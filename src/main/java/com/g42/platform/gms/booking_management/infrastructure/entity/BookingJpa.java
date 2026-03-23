package com.g42.platform.gms.booking_management.infrastructure.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "booking")
@Data
public class BookingJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "booking_code", length = 20, unique = true)
    private String bookingCode;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingEnum status;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "is_guest", nullable = false)
    private Boolean isGuest = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
            name = "booking_details",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private List<CatalogItemJpa> services;
    @Column(name = "queue_order")
    private Integer queueOrder;
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "vehicle_id")
//    private Vehicle vehicle;
    @Column(name = "estimate_time")
    private Integer estimateTime;

}