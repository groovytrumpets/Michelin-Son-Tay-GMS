package com.g42.platform.gms.customer.api.dto;

import com.g42.platform.gms.auth.entity.Gender;
import com.g42.platform.gms.customer.domain.enums.CustomerType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateDto {
    private String fullName;
    private String phone;
    private String email;
    private String pin;
    private Gender gender;
    private String dob;
    private String avatar;
    private CustomerType customerType;
    private String referrerPhone;
    @JsonProperty("isDealer")
    private Boolean isDealer;

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
