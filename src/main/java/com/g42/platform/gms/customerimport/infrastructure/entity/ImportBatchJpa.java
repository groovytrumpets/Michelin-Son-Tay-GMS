package com.g42.platform.gms.customerimport.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Một lần chạy nhập dữ liệu từ file Excel. Tồn tại để hoàn tác được nguyên lô:
 * dữ liệu legacy gần như chắc chắn phải nhập lại vài lần trước khi sạch.
 */
@Entity
@Table(name = "import_batch")
@Data
public class ImportBatchJpa {

    public static final String STATUS_COMMITTED = "COMMITTED";
    public static final String STATUS_ROLLED_BACK = "ROLLED_BACK";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "import_batch_id")
    private Integer importBatchId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "sheet_name", length = 100)
    private String sheetName;

    @Column(name = "imported_by")
    private Integer importedBy;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "status", length = 20)
    private String status = STATUS_COMMITTED;

    /**
     * Thống kê kết quả, kèm danh sách id khách và xe do chính lô này tạo ra —
     * cần cho việc hoàn tác, vì chỉ được xoá những bản ghi lô này sinh ra.
     */
    @Column(name = "counts_json", columnDefinition = "TEXT")
    private String countsJson;

    @Column(name = "note", length = 500)
    private String note;

    /** KEEP_OWNER / TRANSFER / SKIP_VEHICLE — nhớ lại để lần nhập lại giữ nguyên lựa chọn. */
    @Column(name = "plate_conflict_policy", length = 20)
    private String plateConflictPolicy;

    /** Lô mới thay thế lô này sau khi người dùng sửa lại và nhập lại. */
    @Column(name = "replaced_by_batch_id")
    private Integer replacedByBatchId;

    /**
     * Nguyên văn các dòng đã gửi lên lúc ghi. Giữ lại để mở lô ra sửa rồi nhập lại,
     * kể cả những dòng bị bỏ qua nên không nằm trong legacy_visit.
     *
     * Không trả kèm khi liệt kê lô: nội dung này to gấp nhiều lần phần còn lại.
     */
    @JsonIgnore
    @Column(name = "rows_json", columnDefinition = "LONGTEXT")
    private String rowsJson;

    @PrePersist
    protected void onCreate() {
        if (importedAt == null) importedAt = LocalDateTime.now();
    }
}
