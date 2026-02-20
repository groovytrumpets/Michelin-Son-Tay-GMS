package com.g42.platform.gms.booking_management.api.mapper;

import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestDetailRes;
import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestRes;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMRequestDtoMapper {
    BookingRequestRes toBookingRequestRes(BookingRequest bookingRequest);
    List<BookingRequestRes> toBookingRequestRes(List<BookingRequest> bookingRequestList);
    BookingRequestDetailRes toBookingRequestDetailRes(BookingRequest bookingRequest);
}
