package com.g42.platform.gms.booking.customer.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StaffDirectBookingRequest extends BaseBookingRequest {

    @Positive(message = "Estimate ID phai la so duong")
    private Integer estimateId;

    // Không bắt buộc riêng lẻ nữa — cần ít nhất phone HOẶC licensePlate (validate ở BookingService).
    @Pattern(regexp = "^(0[0-9]{9,10})?$", message = "So dien thoai phai bat dau bang 0 va co 10-11 chu so")
    private String phone;

    private String licensePlate;

    @NotBlank(message = "Ten khach hang la bat buoc")
    @Size(min = 2, max = 100, message = "Ten khach hang phai tu 2 den 100 ky tu")
    private String fullName;

    private Boolean isPartsSale;

    private String referrerPhone;

    // ── Phân công sẵn (tuỳ chọn) ────────────────────────────────────────────
    // Lễ tân có thể chọn ngay lúc tạo lịch; bỏ trống thì chọn khi check-in.
    private Integer vehicleId;

    private Integer advisorId;

    private Integer technicianId;

    // Loại xe khách đọc qua điện thoại, chọn từ danh mục hãng/dòng xe có sẵn (hoặc gõ tay nếu
    // không có trong danh mục) — không bắt buộc. Dùng để: (1) lưu ghi chú tham khảo trên booking,
    // (2) bổ sung brand/model/manufactureYear cho Vehicle thật nếu đang trống (không ghi đè dữ liệu đã có).
    @Size(max = 50, message = "Hang xe khong duoc qua 50 ky tu")
    private String vehicleBrand;

    @Size(max = 50, message = "Dong xe khong duoc qua 50 ky tu")
    private String vehicleModel;

    private Integer vehicleYear;
}
