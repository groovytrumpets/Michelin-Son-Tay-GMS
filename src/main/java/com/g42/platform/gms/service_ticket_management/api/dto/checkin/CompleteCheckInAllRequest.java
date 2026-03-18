package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for single-page check-in form.
 * Contains all information needed to complete check-in in one API call.
 */
@Data
public class CompleteCheckInAllRequest {
    
    // Booking information
    @NotNull(message = "Booking ID là bắt buộc")
    private Integer bookingId;
    
    @NotNull(message = "Customer ID là bắt buộc")
    private Integer customerId;
    
    // Vehicle information - REQUIRED: must select existing vehicle
    @NotNull(message = "Vehicle ID là bắt buộc - vui lòng chọn xe hoặc tạo xe mới trước")
    private Integer vehicleId;
    
    // License plate photo (optional)
    private MultipartFile licensePlatePhoto;
    
    // Vehicle condition photos (at least 1 required)
    private MultipartFile photoFront;
    
    @Size(max = 500, message = "Mô tả ảnh không được quá 500 ký tự")
    private String photoFrontDescription;
    
    private MultipartFile photoRear;
    
    @Size(max = 500, message = "Mô tả ảnh không được quá 500 ký tự")
    private String photoRearDescription;
    
    private MultipartFile photoLeftSide;
    
    @Size(max = 500, message = "Mô tả ảnh không được quá 500 ký tự")
    private String photoLeftSideDescription;
    
    private MultipartFile photoRightSide;
    
    @Size(max = 500, message = "Mô tả ảnh không được quá 500 ký tự")
    private String photoRightSideDescription;
    
    private MultipartFile photoInterior;
    
    @Size(max = 500, message = "Mô tả ảnh không được quá 500 ký tự")
    private String photoInteriorDescription;
    
    private MultipartFile photoDamage;      // Optional
    
    @Size(max = 500, message = "Mô tả ảnh không được quá 500 ký tự")
    private String photoDamageDescription;
    
    // Odometer reading (optional)
    @Max(value = 9999999, message = "Số km không hợp lệ")
    private Integer odometerReading;
    
    // Check-in notes (optional)
    @Size(max = 1000, message = "Ghi chú không được quá 1000 ký tự")
    private String checkInNotes;

    /**
     * true  = kích hoạt kiểm tra an toàn
     * false / null = bỏ qua kiểm tra an toàn
     */
    private Boolean safetyInspection;

    // Staff ID who performs check-in (receptionist/staff)
    // This will be used for both uploadedBy (photos) and recordedBy (odometer)
    @NotNull(message = "Staff ID là bắt buộc")
    private Integer staffId;
}
