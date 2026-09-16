package com.g42.platform.gms.customerimport.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Một lượt cũ kèm các dòng dịch vụ, cho khối "Lịch sử trước khi dùng phần mềm". */
@Getter
@Setter
@NoArgsConstructor
public class LegacyVisitDetailDto {

    private Integer legacyVisitId;
    private LocalDateTime visitedAt;

    /** false thì giao diện chỉ hiển thị ngày — giờ là do hệ thống điền, không có thật. */
    private Boolean hasTime;

    private LocalDateTime deliveredAt;
    private String legacyTicketCode;
    private String licensePlate;
    private Integer odometer;
    private String customerNote;
    private String servicesText;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;

    /** Tổng tiền trong sổ lệch tổng các dòng — hiển thị dấu hiệu để người dùng biết mà rà lại. */
    private Boolean amountMismatch;

    /** Lô nhập đã sinh ra lượt này — để mở lại lô đó mà sửa từ ngay hồ sơ khách. */
    private Integer importBatchId;
    private String importFileName;

    private Boolean called;
    private Boolean callSuccess;
    private String callNote;

    private List<ImportItemDto> items = new ArrayList<>();
}
