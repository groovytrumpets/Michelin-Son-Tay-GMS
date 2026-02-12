package com.g42.platform.gms.booking.customer.api.mapper;

import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingDtoMapper {

    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "phone", ignore = true)
    BookingResponse toResponse(Booking domain);
}
