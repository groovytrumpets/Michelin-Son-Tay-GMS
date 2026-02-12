package com.g42.platform.gms.booking.customer.api.mapper;

import com.g42.platform.gms.booking.customer.api.dto.BookingRequestResponse;
import com.g42.platform.gms.booking.customer.domain.entity.BookingRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingRequestDtoMapper {

    @Mapping(
            target = "status",
            expression = "java(domain.getStatus() != null ? domain.getStatus().name() : null)"
    )
    BookingRequestResponse toResponse(BookingRequest domain);
}
