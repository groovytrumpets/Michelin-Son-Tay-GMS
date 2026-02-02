package com.g42.platform.gms.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingRequest {
    // Thông tin khách (Dùng cho Guest)
    private String fullName;
    @NotNull(message = "Số điện thoại là bắt buộc")
    private String phoneNumber;
    private String otpCode;

    // Lịch hẹn & Mô tả
    @NotNull
    private LocalDate appointmentDate;
    @NotNull
    private LocalTime appointmentTime;

    private String userNote; // Khách tự nhập mô tả

    // Danh sách dịch vụ chọn thêm (Optional)
    private List<Integer> selectedServiceIds;
}
