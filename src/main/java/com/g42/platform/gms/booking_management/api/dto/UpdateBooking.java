package com.g42.platform.gms.booking_management.api.dto;

import com.g42.platform.gms.booking_management.domain.entity.CatalogItem;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class UpdateBooking {
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private BookingEnum status;
    private List<CatalogItem> services;
}
