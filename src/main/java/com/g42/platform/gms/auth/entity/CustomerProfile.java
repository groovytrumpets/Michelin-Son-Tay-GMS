package com.g42.platform.gms.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_profile")
@Getter
@Setter
public class CustomerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "dob")
    private LocalDate dob;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "first_booking_at")
    private LocalDateTime firstBookingAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "referrer_id")
    private Integer referrerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 20)
    private com.g42.platform.gms.customer.domain.enums.CustomerType customerType;

    @Column(name = "is_dealer")
    private Boolean isDealer;

    @Column(name = "is_company")
    private Boolean isCompany;

    @Column(name = "company_name")
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", length = 10)
    private com.g42.platform.gms.notification.domain.NotificationChannel notificationChannel;
}
