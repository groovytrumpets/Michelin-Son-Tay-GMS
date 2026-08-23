package com.g42.platform.gms.customer.infrastructure.entity;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_auth")
@Getter
@Setter
public class CustomerAuthJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_auth_id")
    private Integer customerAuthId;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "pin_hash")
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CustomerStatus status;

    @Column(name = "failed_attempt_count")
    private Integer failedAttemptCount;

    @Column(name = "otp_attempt_count")
    private Integer otpAttemptCount;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Bắt khách đổi PIN ở lần đăng nhập đầu. Bật cho tài khoản nhập từ sổ cũ, vì PIN
     * khởi tạo là 6 số cuối số điện thoại — ai biết số điện thoại là đoán được PIN.
     */
    @Column(name = "must_change_pin")
    private Boolean mustChangePin = false;
}
