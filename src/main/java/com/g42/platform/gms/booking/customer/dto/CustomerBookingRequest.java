package com.g42.platform.gms.booking.customer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CustomerBookingRequest {
    
    @NotNull(message = "Ngày hẹn là bắt buộc")
    @FutureOrPresent(message = "Ngày hẹn phải là hôm nay hoặc tương lai")
    private LocalDate appointmentDate;
    
    @NotNull(message = "Giờ hẹn là bắt buộc")
    private LocalTime appointmentTime;
    
    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    private String userNote;
    
    @Size(max = 10, message = "Chỉ được chọn tối đa 10 dịch vụ")
    private List<@Positive(message = "ID dịch vụ phải là số dương") Integer> selectedServiceIds;
    
    @Positive(message = "ID xe phải là số dương")
    private Integer vehicleId; // Không bắt buộc, xưởng sẽ nhập sau
    
    @jakarta.validation.constraints.AssertTrue(message = "Phải nhập mô tả hoặc chọn ít nhất 1 dịch vụ")
    public boolean isValidRequest() {
        return (userNote != null && !userNote.trim().isEmpty()) 
            || (selectedServiceIds != null && !selectedServiceIds.isEmpty());
    }
}
