package com.g42.platform.gms.booking_management.api.dto.requesting;

import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.api.dto.CatalogItemRes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestUpdateReq {
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String serviceCategory;
    private Boolean isGuest;
    private List<Integer> services;
}
