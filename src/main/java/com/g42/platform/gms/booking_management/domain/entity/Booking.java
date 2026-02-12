package com.g42.platform.gms.booking_management.domain.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingDetailJpa;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    private Integer bookingId;
    private CustomerProfile customer;
    private Vehicle vehicle;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private BookingEnum status;
    private String description;
    private Boolean isGuest = false;
    private LocalDateTime createdAt;
    private List<CatalogItem> services;


}