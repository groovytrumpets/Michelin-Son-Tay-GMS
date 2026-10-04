package com.g42.platform.gms.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bộ "In chứng từ" của một màn hình nghiệp vụ: những dạng chứng từ nào hiện
 * trong popup in, theo thứ tự nào, dạng nào chọn sẵn.
 *
 * {@code screenCode} do frontend định nghĩa, backend chỉ lưu. {@code kindCodes} là
 * mảng JSON có thể lẫn mã phiếu viết tay cũ (LEGACY_*) không nằm trong
 * document_kind, nên cố tình không ràng buộc khoá ngoại.
 */
@Entity
@Table(name = "document_print_set")
@Getter
@Setter
public class DocumentPrintSet {

    @Id
    @Column(name = "screen_code", length = 50)
    private String screenCode;

    @Column(name = "kind_codes", columnDefinition = "TEXT", nullable = false)
    private String kindCodes;

    @Column(name = "default_kind_code", length = 50)
    private String defaultKindCode;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}
