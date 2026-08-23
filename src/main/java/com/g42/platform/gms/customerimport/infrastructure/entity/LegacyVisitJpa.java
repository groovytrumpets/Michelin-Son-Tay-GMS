package com.g42.platform.gms.customerimport.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một lượt khách đến xưởng theo sổ Excel cũ, trước khi xưởng dùng phần mềm.
 *
 * Cố ý KHÔNG dùng lại service_ticket: lượt cũ không có báo giá, phân công hay
 * thanh toán, nên nhét vào bảng phiếu sẽ làm sai mọi truy vấn vận hành và báo
 * cáo. Xem phần đầu changeset 022-legacy-visit.yaml.
 */
@Entity
@Table(name = "legacy_visit")
@Data
public class LegacyVisitJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "legacy_visit_id")
    private Integer legacyVisitId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "vehicle_id")
    private Integer vehicleId;

    /** Nguồn tính lần cuối khách đến xưởng. */
    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    /** false = sổ cũ chỉ ghi ngày, phần giờ do hệ thống điền — đừng hiển thị như giờ thật. */
    @Column(name = "has_time")
    private Boolean hasTime = false;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "legacy_ticket_code", length = 100)
    private String legacyTicketCode;

    @Column(name = "odometer")
    private Integer odometer;

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;

    /** Toàn bộ dịch vụ đã dùng gộp thành chuỗi, để hiển thị nhanh không cần join. */
    @Column(name = "services_text", columnDefinition = "TEXT")
    private String servicesText;

    /** Chỉ để tham khảo — KHÔNG đưa vào báo cáo doanh thu. */
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "amount_mismatch")
    private Boolean amountMismatch = false;

    @Column(name = "called")
    private Boolean called = false;

    @Column(name = "call_success")
    private Boolean callSuccess = false;

    @Column(name = "call_note", length = 500)
    private String callNote;

    @Column(name = "import_batch_id", nullable = false)
    private Integer importBatchId;

    @Column(name = "source_row_no")
    private Integer sourceRowNo;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

    /** Khoá chống nhập trùng, ghép ở tầng ứng dụng. Xem ImportNormalizer.dedupeKey. */
    @Column(name = "dedupe_key", length = 120, nullable = false)
    private String dedupeKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
