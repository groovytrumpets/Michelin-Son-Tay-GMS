package com.g42.platform.gms.booking.customer.api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public abstract class BaseBookingRequest extends ReminderRequest {
    
    @NotNull(message = "Ngày hẹn là bắt buộc")
    @FutureOrPresent(message = "Ngày hẹn phải là hôm nay hoặc tương lai")
    protected LocalDate appointmentDate;
    
    @NotNull(message = "Giờ hẹn là bắt buộc")
    protected LocalTime appointmentTime;
    
    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    protected String userNote;
    
    @Size(max = 10, message = "Chỉ được chọn tối đa 10 dịch vụ")
    protected List<@Positive(message = "ID dịch vụ phải là số dương") Integer> selectedServiceIds;

    @Size(max = 20, message = "Số điện thoại người giới thiệu không hợp lệ")
    protected String referrerPhone;

    /** Xưởng khách hẹn tới (Liquibase 044). Bỏ trống = xưởng máy đang chọn / xưởng mặc định. */
    @Positive(message = "Xưởng không hợp lệ")
    protected Integer branchId;
}
