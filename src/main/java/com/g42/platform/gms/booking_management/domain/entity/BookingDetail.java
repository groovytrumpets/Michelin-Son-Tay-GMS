package com.g42.platform.gms.booking_management.domain.entity;

import com.g42.platform.gms.booking.entity.Booking;
import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpaEntity;
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
    private CatalogItemJpaEntity item;


}