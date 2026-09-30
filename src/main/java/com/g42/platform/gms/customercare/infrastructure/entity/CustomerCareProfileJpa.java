package com.g42.platform.gms.customercare.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Điều cần nhớ mỗi lần gọi một khách ("chỉ rảnh cuối tuần", "gọi sau 17h"). Changeset 045-2.
 *
 * Tách khỏi ghi chú từng cuộc gọi: ghi chú cuộc gọi là chuyện của lần đó, còn cái này
 * nhân viên nào gọi lần sau cũng phải đọc trước.
 */
@Entity
@Table(name = "customer_care_profile")
@Data
public class CustomerCareProfileJpa {

    @Id
    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "care_note", length = 1000)
    private String careNote;

    @Column(name = "preferred_time", length = 100)
    private String preferredTime;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
