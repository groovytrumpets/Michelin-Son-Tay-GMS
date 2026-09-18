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
     * Template JSON của pdfme: khổ giấy, danh sách ô và toạ độ, và cả nền PDF
     * dạng base64 nếu mẫu được dựng từ file Word xuất sang PDF.
     *
     * Backend cố tình KHÔNG hiểu nội dung chuỗi này — nó chỉ lưu và trả lại
     * nguyên vẹn. Nhờ vậy thêm loại ô mới cho trình thiết kế chỉ cần sửa
     * frontend, không phải đụng tới backend hay chạy migration.
     */
    @Lob
    @Column(name = "layout_json", columnDefinition = "LONGTEXT")
    private String layoutJson;

    /** Tên file Word gốc nếu mẫu được nhập vào, để về sau còn biết nguồn gốc. */
    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(name = "paper_size", length = 20)
    private String paperSize = "A4";

    /**
     * Ba số liệu tính sẵn lúc lưu để danh sách mẫu không phải nạp layoutJson.
     * Chỉ dùng để hiển thị, không tham gia nghiệp vụ nào.
     */
    @Column(name = "page_count")
    private Integer pageCount = 0;

    @Column(name = "field_count")
    private Integer fieldCount = 0;

    @Column(name = "has_base_pdf")
    private Boolean hasBasePdf = false;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "is_default")
    private Boolean defaultTemplate = false;

    @Column(name = "is_active")
    private Boolean active = true;

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
