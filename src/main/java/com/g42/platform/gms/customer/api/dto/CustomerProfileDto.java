package com.g42.platform.gms.customer.api.dto;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.auth.entity.Gender;
import com.g42.platform.gms.customer.domain.enums.CustomerType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class CustomerProfileDto {
    private Integer customerId;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dob;
    private Gender gender;
    private String avatar;
    private LocalDateTime firstBookingAt;
    private LocalDateTime createdAt;
    private CustomerType customerType;
    private CustomerStatus status;
    @JsonProperty("isDealer")
    private Boolean isDealer;
    private Integer totalBookings;
    private Integer totalPoints;
    private String currentRank;
    private String currentDealerRank;
    @JsonProperty("isCompany")
    private Boolean isCompany;
    private String companyName;
    private com.g42.platform.gms.notification.domain.NotificationChannel notificationChannel;

    // ── Danh bạ đối tác ─────────────────────────────────────────────────────
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
    private String customerGroupName;
    private String note;
    private String representativeName;
    private String repIdentityCard;
    private String position;
    private String contractNumber;
    private LocalDate contractDate;
    private String bankAccountInfo;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String contactAddress;
}
