package com.g42.platform.gms.booking_management.api.dto;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking_management.domain.entity.BookingDetail;
import com.g42.platform.gms.booking_management.domain.entity.CatalogItem;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingDetailJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

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
