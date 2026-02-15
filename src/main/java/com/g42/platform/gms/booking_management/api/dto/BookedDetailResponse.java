package com.g42.platform.gms.booking_management.api.dto;

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
public class BookedDetailResponse {
    private Integer bookingId;
    private CustomerDto customer;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private BookingEnum status;
    private String description;
    private Boolean isGuest;
    private LocalDateTime createdAt;
    private List<CatalogItemRes> items;
}
