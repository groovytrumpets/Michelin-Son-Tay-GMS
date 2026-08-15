package com.g42.platform.gms.customer.domain.entity;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.auth.entity.Gender;
import com.g42.platform.gms.customer.domain.enums.CustomerRank;
import com.g42.platform.gms.customer.domain.enums.CustomerType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfile {
    private Integer customerId;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dob;
    private Gender gender;
    private String avatar;
    private CustomerType customerType;
    private CustomerStatus status;
    private LocalDateTime firstBookingAt;
    private LocalDateTime createdAt;
    // Ranking fields (populated from customer_points table)
    private CustomerRank currentRank;
    private Integer totalPoints;
    private Integer totalBookings;
    private String currentDealerRank;
    private Integer referrerId;
    @JsonProperty("isDealer")
    private Boolean isDealer = false;
    @JsonProperty("isCompany")
    private Boolean isCompany = false;
    @JsonProperty("companyName")
    private String companyName = "";
    private com.g42.platform.gms.notification.domain.NotificationChannel notificationChannel;

    // ── Danh bạ đối tác: thông tin chung ────────────────────────────────────
    private String customerCode;
    private String taxCode;
    private String provinceId;
    private String provinceName;
    private String districtId;
    private String districtName;
    private String wardId;
    private String wardName;
    private String address;
    private String identityCard;
    private LocalDate idIssueDate;
    private String idIssuePlace;
    private Integer customerGroupId;
    /** Tên nhóm khách hàng, nạp kèm để hiển thị (không lưu ở customer_profile). */
    private String customerGroupName;
    private String note;

    // ── Danh bạ đối tác: pháp nhân ──────────────────────────────────────────
    private String representativeName;
    private String repIdentityCard;
    private String position;
    private String contractNumber;
    private LocalDate contractDate;
    private String bankAccountInfo;
    private BigDecimal latitude;
    private BigDecimal longitude;

    // ── Danh bạ đối tác: liên hệ khác ───────────────────────────────────────
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String contactAddress;
}
