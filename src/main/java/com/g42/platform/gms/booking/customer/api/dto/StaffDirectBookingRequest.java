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

    // Loại xe khách đọc qua điện thoại (VD: "Honda Air Blade 2020") — chỉ là ghi chú tham khảo,
    // không bắt buộc và không thay thế cho việc chọn/đăng ký xe thật ở check-in.
    @Size(max = 100, message = "Loai xe khong duoc qua 100 ky tu")
    private String vehicleTypeNote;
}
