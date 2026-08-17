package com.g42.platform.gms.marketing.recruitment.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Một khoản hỗ trợ của vị trí ("Phụ cấp ăn trưa" — "30.000đ/bữa").
 *
 * <p>Tách thành bảng riêng thay vì gộp vào đoạn HTML quyền lợi vì thẻ vị trí
 * ngoài trang danh sách cần đọc được từng khoản để xếp thành chip — thứ không
 * làm được nếu tất cả nằm chung một khối HTML tự do.
 */
@Getter
@Setter
@Entity
@Table(name = "job_benefit")
public class JobBenefitJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "benefit_id", nullable = false)
    private Long benefitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPositionJpa job;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "value_text", length = 250)
    private String valueText;

    /** Tên icon lucide do FE quy ước, vd "wallet". */
    @Column(name = "icon", length = 40)
    private String icon;

    @Column(name = "display_order")
    private Integer displayOrder;
}
