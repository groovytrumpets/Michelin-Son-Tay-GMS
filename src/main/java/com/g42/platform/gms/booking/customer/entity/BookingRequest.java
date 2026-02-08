package com.g42.platform.gms.booking.customer.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffProfile;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "booking_request", indexes = {
    @Index(name = "idx_phone", columnList = "phone"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created", columnList = "created_at"),
    @Index(name = "idx_customer", columnList = "customer_id"),
    @Index(name = "idx_expires", columnList = "expires_at")
})
@Data
public class BookingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "service_category", length = 50)
    private String serviceCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingRequestStatus status = BookingRequestStatus.PENDING;

    @Column(name = "is_guest", nullable = false)
    private Boolean isGuest = true;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @ManyToOne
    @JoinColumn(name = "confirmed_by")
    private StaffProfile confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingRequestDetail> details;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingRequestStatus.PENDING;
        }
        if (isGuest == null) {
            isGuest = true;
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(24);
        }
    }
}
