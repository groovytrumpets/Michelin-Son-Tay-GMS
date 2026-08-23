package com.g42.platform.gms.customerimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Một dòng trong sổ cũ = một lượt khách đến xưởng, đã được FE đọc và làm phẳng.
 *
 * FE gửi giá trị gần với nguyên bản (chưa chuẩn hoá số điện thoại, biển số...);
 * việc chuẩn hoá làm ở server để bước kiểm tra thử và bước ghi luôn cho ra cùng
 * một kết quả, không phụ thuộc phiên bản FE nào đang chạy.
 */
@Getter
@Setter
@NoArgsConstructor
public class ImportRowDto {

    /** Số dòng trong file gốc, để người dùng tìm lại được khi báo lỗi. */
    private Integer sourceRowNo;

    /** Mã hoặc số thứ tự phiếu trong sổ cũ. */
    private String legacyTicketCode;

    private String fullName;
    private String phone;
    private String email;

    private String licensePlate;
    private String brand;
    private String model;
    private Integer manufactureYear;
    private Integer odometer;

    /** yyyy-MM-dd. Sổ cũ không có giờ nên chỉ nhận ngày. */
    private String visitedDate;
    private String deliveredDate;

    private String customerNote;
    private BigDecimal totalAmount;

    @JsonProperty("called")
    private Boolean called;

    @JsonProperty("callSuccess")
    private Boolean callSuccess;

    private String callNote;

    private List<ImportItemDto> items = new ArrayList<>();

    /** Nguyên văn dòng Excel, giữ lại để tra ngược khi nghi ngờ dữ liệu đã nhập sai. */
    private String rawJson;
}
