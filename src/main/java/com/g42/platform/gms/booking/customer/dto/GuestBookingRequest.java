package com.g42.platform.gms.booking.customer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class GuestBookingRequest {
    
    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại phải bắt đầu bằng 0 và có 10-11 chữ số")
    private String phone;
    
    @NotBlank(message = "Tên khách hàng là bắt buộc")
    @Size(min = 2, max = 100, message = "Tên khách hàng phải từ 2 đến 100 ký tự")
    private String fullName;
    
    @NotNull(message = "Ngày hẹn là bắt buộc")
    @FutureOrPresent(message = "Ngày hẹn phải là hôm nay hoặc tương lai")
    private LocalDate appointmentDate;
    
    @NotNull(message = "Giờ hẹn là bắt buộc")
    private LocalTime appointmentTime;
    
    private String userNote; // Mô tả tình trạng xe
    
    private List<Integer> selectedServiceIds; // Danh sách ID dịch vụ
    
    @jakarta.validation.constraints.AssertTrue(message = "Phải nhập mô tả hoặc chọn ít nhất 1 dịch vụ")
    public boolean isValidRequest() {
        return (userNote != null && !userNote.trim().isEmpty()) 
            || (selectedServiceIds != null && !selectedServiceIds.isEmpty());
    }
}
