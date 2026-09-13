package com.g42.platform.gms.booking.customer.infrastructure.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "booking")
@Data
public class BookingJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "booking_code", length = 20, unique = true)
    private String bookingCode;

    @ManyToOne 
    @JoinColumn(name = "customer_id", nullable = true)
    private CustomerProfile customer;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "is_guest", nullable = false)
    private Boolean isGuest = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "queue_order")
    private Integer queueOrder;
    @Column(name = "estimate_id")
    private Integer estimateId;

    @Column(name = "is_parts_sale", nullable = false)
    private Boolean isPartsSale = false;

    // ── Phân công sẵn khi tạo lịch (đều không bắt buộc) ─────────────────────
    // Để trống thì lễ tân chọn lúc check-in như trước.
    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @Column(name = "advisor_id")
    private Integer advisorId;

    @Column(name = "technician_id")
    private Integer technicianId;

    @Column(name = "vehicle_type_note", length = 100)
    private String vehicleTypeNote;

    @ManyToMany
    @JoinTable(
            name = "booking_details",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private List<CatalogItemJpaEntity> services;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
        if (isGuest == null) {
            isGuest = false;
        }
        if (isPartsSale == null) {
            isPartsSale = false;
        }
    }
}
