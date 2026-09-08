package com.g42.platform.gms.billing.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một ảnh/video chứng từ thanh toán đính kèm cho một phiếu dịch vụ.
 *
 * <p>File được đẩy lên Cloudinary ở phía FE trước, ở đây chỉ lưu URL + loại để
 * giao diện biết render {@code <img>} hay {@code <video>}. Gắn theo
 * {@code service_ticket_id} (không phải bill) để màn thanh toán và màn chi tiết
 * phiếu dùng chung một rổ chứng từ.
 */
@Getter
@Setter
@Entity
@Table(name = "payment_proof")
public class PaymentProofJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_proof_id", nullable = false)
    private Integer paymentProofId;

    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;

    /** Hoá đơn tại thời điểm thêm chứng từ; để trống nếu chưa có bill. */
    @Column(name = "bill_id")
    private Integer billId;

    /** IMAGE hoặc VIDEO. */
    @Column(name = "media_type", nullable = false, length = 16)
    private String mediaType;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "public_id", length = 255)
    private String publicId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "uploaded_by")
    private Integer uploadedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
