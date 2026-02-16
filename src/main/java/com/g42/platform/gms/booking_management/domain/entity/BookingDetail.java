package com.g42.platform.gms.booking_management.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {

    private Integer id;
    private Booking booking;
    private CatalogItem item;

}