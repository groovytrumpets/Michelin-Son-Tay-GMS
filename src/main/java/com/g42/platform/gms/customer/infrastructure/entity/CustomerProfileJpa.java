package com.g42.platform.gms.customer.infrastructure.entity;

import com.g42.platform.gms.auth.entity.Gender;
import com.g42.platform.gms.customer.domain.enums.CustomerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 20)
    private CustomerType customerType = CustomerType.INDIVIDUAL;

    @Column(name = "first_booking_at")
    private LocalDateTime firstBookingAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "referrer_id")
    private Integer referrerId;

    @Column(name = "is_dealer")
    private Boolean isDealer = false;

    @Column(name = "is_company")
    private Boolean isCompany = false;

    @Column(name = "company_name", length = 255)
    private String companyName;

    // ── Danh bạ đối tác: thông tin chung ────────────────────────────────────
    @Column(name = "customer_code", length = 50)
    private String customerCode;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "province_id", length = 20)
    private String provinceId;

    @Column(name = "province_name", length = 150)
    private String provinceName;

    @Column(name = "district_id", length = 20)
    private String districtId;

    @Column(name = "district_name", length = 150)
    private String districtName;

    @Column(name = "ward_id", length = 20)
    private String wardId;

    @Column(name = "ward_name", length = 150)
    private String wardName;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "identity_card", length = 30)
    private String identityCard;

    @Column(name = "id_issue_date")
    private LocalDate idIssueDate;

    @Column(name = "id_issue_place")
    private String idIssuePlace;

    @Column(name = "customer_group_id")
    private Integer customerGroupId;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // ── Danh bạ đối tác: pháp nhân ──────────────────────────────────────────
    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "rep_identity_card", length = 30)
    private String repIdentityCard;

    /** Chức vụ người đại diện. Cột đặt tên rep_position vì POSITION là từ khoá MySQL. */
    @Column(name = "rep_position", length = 150)
    private String position;

    @Column(name = "contract_number", length = 100)
    private String contractNumber;

    @Column(name = "contract_date")
    private LocalDate contractDate;

    @Column(name = "bank_account_info", length = 500)
    private String bankAccountInfo;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    // ── Danh bạ đối tác: liên hệ khác ───────────────────────────────────────
    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_address", length = 500)
    private String contactAddress;
}
