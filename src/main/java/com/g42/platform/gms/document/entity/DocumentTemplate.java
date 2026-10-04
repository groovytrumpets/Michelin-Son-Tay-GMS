package com.g42.platform.gms.document.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bố cục một mẫu chứng từ.
 *
 * Một dạng chứng từ có thể có nhiều mẫu (mẫu cho khách lẻ, mẫu cho khách doanh
 * nghiệp, mẫu rút gọn...), đúng một mẫu được đánh dấu mặc định để màn nghiệp vụ
 * chọn sẵn.
 */
@Entity
@Table(name = "document_template")
@Getter
@Setter
public class DocumentTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Integer templateId;

    @Column(name = "kind_id", nullable = false)
    private Integer kindId;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Danh sách khối của mẫu, dạng JSON.
     *
     * Backend cố tình KHÔNG hiểu nội dung chuỗi này — nó chỉ lưu và trả lại
     * nguyên vẹn, ngoài việc đếm số khối để hiển thị. Nhờ vậy thêm loại khối mới
     * cho trình dựng chỉ cần sửa frontend, không phải đụng backend hay chạy
     * migration.
     */
    @Lob
    @Column(name = "layout_json", columnDefinition = "LONGTEXT")
    private String layoutJson;

    @Column(name = "paper_size", length = 20)
    private String paperSize = "A4";

    @Column(name = "note", length = 500)
    private String note;

    /** Đếm sẵn lúc lưu để danh sách mẫu không phải nạp cả layoutJson. */
    @Column(name = "block_count")
    private Integer blockCount = 0;

    @Column(name = "is_default")
    private Boolean defaultTemplate = false;

    @Column(name = "is_active")
    private Boolean active = true;

    /**
     * Mẫu gốc dựng sẵn theo bộ file Word của xưởng. Sửa được nhưng KHÔNG xoá
     * được, để dạng chứng từ luôn còn một mẫu mà in. Mỗi dạng có tối đa một mẫu
     * gốc; bản sao của nó là mẫu thường.
     */
    @Column(name = "is_system")
    private Boolean system = false;

    /** Tăng mỗi lần lưu bố cục, để về sau đối chiếu với bản chứng từ đã in. */
    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "created_by")
    private Integer createdBy;

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
