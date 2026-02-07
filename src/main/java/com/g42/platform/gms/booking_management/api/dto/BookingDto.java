package com.g42.platform.gms.booking_management.api.dto;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.vehicle.entity.Vehicle;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingDto {
    private Integer id;
    private CustomerProfile customer;
    private Vehicle vehicle;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private BookingEnum status;
    private Instant createdAt;
}
