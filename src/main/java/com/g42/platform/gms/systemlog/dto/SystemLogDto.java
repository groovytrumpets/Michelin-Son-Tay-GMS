package com.g42.platform.gms.systemlog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogDto {
    private Long logId;
    private Integer actorStaffId;
    private String actorName;
    private String actorRole;
    private String action;
    private String module;
    private String severity;
    /** Mô tả thô đúng như trong DB (giữ để đối chiếu / xuất CSV). */
    private String description;
    /** Câu tiếng Việt dễ đọc, ví dụ "Sửa mặt hàng (danh mục & tồn kho)". */
    private String summary;
    /** Các thay đổi trước → sau, ví dụ "Tồn kho CS1 TÂN PHÚC: 40 → 0"; rỗng với nhật ký cũ. */
    private List<String> changes;
    /** Đường dẫn API gốc, ví dụ "PUT /api/warehouse/catalog-item/update/1306"; null với nhật ký ghi tay. */
    private String api;
    /** Thao tác vặt (đọc thông báo, lưu cột bảng...) — FE làm mờ. */
    private boolean noise;
    private String targetType;
    private String targetId;
    /** Loại đối tượng nhận ra từ đường dẫn (CATALOG_ITEM, STOCK_ENTRY...), dùng cho nút "xem lịch sử đối tượng". */
    private String targetKind;
    /**
     * Tên dễ đọc của đối tượng (tên mặt hàng, mã phiếu...) tra lúc đọc nhật ký,
     * không lưu trong system_log. Null nếu không nhận ra loại đối tượng.
     */
    private String targetName;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
