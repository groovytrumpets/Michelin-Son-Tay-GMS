package com.g42.platform.gms.customer.infrastructure.entity;

import com.g42.platform.gms.auth.entity.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_profile")
@Getter
@Setter
public class CustomerProfileJpa {

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
}
