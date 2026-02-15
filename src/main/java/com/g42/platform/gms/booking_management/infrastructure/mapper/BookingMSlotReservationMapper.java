package com.g42.platform.gms.booking_management.infrastructure.mapper;

import com.g42.platform.gms.booking_management.domain.entity.BookingSlotReservation;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingSlotReservationJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMSlotReservationMapper {
    BookingSlotReservation toDomain(BookingSlotReservationJpa reservation);
    BookingSlotReservationJpa toJpa(BookingSlotReservation bookingSlotReservation);
}
