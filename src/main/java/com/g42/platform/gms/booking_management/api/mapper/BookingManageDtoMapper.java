package com.g42.platform.gms.booking_management.api.mapper;

import com.g42.platform.gms.booking_management.api.dto.BookedRespond;
import com.g42.platform.gms.booking_management.domain.entity.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingManageDtoMapper {
    BookedRespond toBookedRespond(Booking booking);
}
