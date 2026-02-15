package com.g42.platform.gms.booking_management.api.dto;

import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookedRespond {
    private Integer bookingId;
    private CustomerDto customer;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private BookingEnum status;
    private String description;
    private Boolean isGuest;
    private LocalDateTime createdAt;
}
