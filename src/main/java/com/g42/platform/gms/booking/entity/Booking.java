package com.g42.platform.gms.booking.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "booking")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer bookingId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    // 🔥 CHO PHÉP NULL: Vì khách đặt lịch chưa cần nhập xe
    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = true)
    private Vehicle vehicle;

    private LocalDate scheduledDate;
    private LocalTime scheduledTime;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private Boolean isGuest;

    @ManyToMany
    @JoinTable(
            name = "booking_details",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private List<CatalogItemJpaEntity> services;
}