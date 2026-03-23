package com.g42.platform.gms.booking_management.domain.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
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
public class Booking {

    private Integer bookingId;
    private String bookingCode;
    private Integer customerId;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private BookingEnum status;
    private String description;
    private Boolean isGuest = false;
    private LocalDateTime createdAt;
    private List<CatalogItem> services;
    private Integer queueOrder;
    private Integer estimateTime;

}