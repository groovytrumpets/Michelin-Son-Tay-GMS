package com.g42.platform.gms.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Dạng chứng từ — "Hợp đồng sửa chữa", "Phiếu bảo hành", "Hoá đơn bán hàng"...
 *
 * Là DỮ LIỆU chứ không phải enum, để người dùng tự thêm dạng mới ở
 * /document-template mà không phải gọi lập trình viên. Cái buộc phải có sẵn
 * trong code chỉ là {@link DataSourceType} — nó quyết định dạng này có những
 * trường nào để kéo vào mẫu.
 */
@Entity
@Table(name = "document_kind")
@Getter
@Setter
public class DocumentKind {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kind_id")
    private Integer kindId;

    /**
     * Mã ổn định để code trỏ tới ("PAYMENT_RECEIPT"). Người dùng đổi được tên
     * hiển thị nhưng không đổi được mã của dạng hệ thống.
     */
    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", length = 30, nullable = false)
    private DataSourceType dataSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 30, nullable = false)
    private DocumentStage stage;

    /**
     * Khuôn số hiệu chứng từ. {@code {ticketCode}} được thay bằng mã phiếu, nên
     * số hiệu chứng từ bám theo số phiếu đúng như nghiệp vụ ở xưởng đang làm.
     */
    @Column(name = "doc_no_pattern", length = 100)
    private String docNoPattern;

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Dạng do hệ thống nạp sẵn. Sửa tên và mẫu thì được, xoá thì không — có chỗ
     * trong code trỏ tới bằng {@link #code}.
     */
    @Column(name = "is_system")
    private Boolean system = false;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
