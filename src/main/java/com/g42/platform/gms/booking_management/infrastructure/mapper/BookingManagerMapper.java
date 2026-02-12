package com.g42.platform.gms.booking_management.infrastructure.mapper;

import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.booking_management.domain.entity.BookingDetail;
import com.g42.platform.gms.booking_management.domain.entity.CatalogItem;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingDetailJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingManagerMapper {
    Booking toDomain(BookingJpa bookingJpa);
    List<Booking> toBookingJpa(List<BookingJpa> bookingJpa);
}
