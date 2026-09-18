package com.g42.platform.gms.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Hồ sơ công ty in ở đầu mọi chứng từ.
 *
 * Luôn chỉ có đúng một hàng (id = 1). Trước đây tên công ty, mã số thuế, số tài
 * khoản nằm viết cứng trong các component in ở frontend; đổi một chữ phải sửa
 * mã nguồn và build lại.
 */
@Entity
@Table(name = "company_profile")
@Getter
@Setter
public class CompanyProfile {

    public static final Integer SINGLETON_ID = 1;

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    /** Tên hiển thị ngắn trên chứng từ, VD "TRUNG TÂM MICHELIN CAR SERVICE SƠN TÂY". */
    @Column(name = "short_name")
    private String shortName;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "representative")
    private String representative;

    @Column(name = "representative_title", length = 100)
    private String representativeTitle;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

    /** Mỗi dòng một tài khoản; in thẳng xuống chứng từ nên để tự do, không tách bảng. */
    @Column(name = "bank_accounts", columnDefinition = "TEXT")
    private String bankAccounts;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
