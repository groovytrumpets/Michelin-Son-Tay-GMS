package com.g42.platform.gms.booking_management.api.dto;

import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequestDetail;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class BookingRequestRes {
    private Integer requestId;
    private String phone;
    private String fullName;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private BookingRequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String clientIp;
    private List<BookingRequestDetail> details;
}
