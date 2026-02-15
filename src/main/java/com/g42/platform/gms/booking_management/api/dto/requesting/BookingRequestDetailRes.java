package com.g42.platform.gms.booking_management.api.dto.requesting;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.api.dto.CatalogItemRes;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequestDetail;
import com.g42.platform.gms.booking_management.domain.entity.CatalogItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDetailRes {
    private Integer requestId;
    private String phone;
    private String fullName;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String serviceCategory;
    private BookingRequestStatus status;
    private Boolean isGuest;
    private CustomerProfile customer;
    private StaffProfile confirmedBy;
    private LocalDateTime confirmedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private List<CatalogItemRes> services;
}
