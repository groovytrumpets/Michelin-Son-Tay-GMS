package com.g42.platform.gms.booking.customer.domain.entity;

import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Booking {
    private Integer bookingId;
    private String bookingCode;
    private Integer customerId;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private BookingStatus status;
    private String description;
    private Boolean isGuest = false;
    private LocalDateTime createdAt;
    private Integer queueOrder;
    private Integer estimateId;
    private List<Integer> catalogItemIds = new ArrayList<>();
    private Boolean isPartsSale = false;
    /** Xe + nhân sự phân công sẵn từ lúc tạo lịch; null nghĩa là chọn khi check-in. */
    private Integer vehicleId;
    private Integer advisorId;
    private Integer technicianId;
    /** Loại xe khách đọc qua điện thoại lúc tạo lịch (khi chưa có/chưa chọn xe trong hệ thống). */
    private String vehicleTypeNote;
    /** Số km ước tính khách đọc qua điện thoại lúc tạo lịch — chỉ để tham khảo, không phải số đo thật. */
    private Integer odometerEstimate;
    
    public void initializeDefaults() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.CONFIRMED;
        }
        if (isGuest == null) {
            isGuest = false;
        }
        if (isPartsSale == null) {
            isPartsSale = false;
        }
    }
}
