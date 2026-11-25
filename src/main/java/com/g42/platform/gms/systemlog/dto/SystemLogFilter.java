package com.g42.platform.gms.systemlog.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** Bộ lọc dùng chung cho danh sách, thống kê và xuất CSV của Nhật ký hệ thống (bind từ query string). */
@Data
public class SystemLogFilter {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;
    private String role;
    private String action;
    private String severity;
    private String module;
    /** Tìm trong tên người làm, nội dung, mã đối tượng; kèm tên mặt hàng / mã phiếu của nhật ký cũ. */
    private String search;
    /** Lọc theo một nhân viên cụ thể. */
    private Integer actorStaffId;
    /** Mã nhóm thao tác, xem SystemLogOperation. */
    private String operation;
    /** Ẩn thao tác vặt (đọc thông báo, lưu cột bảng, khách xem bài viết...). */
    private Boolean hideNoise;
    /** Xem toàn bộ lịch sử của một đối tượng: loại (CATALOG_ITEM, STOCK_ENTRY...) + id. */
    private String targetKind;
    private String targetId;
}
