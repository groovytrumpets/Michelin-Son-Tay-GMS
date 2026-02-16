package com.g42.platform.gms.booking_management.infrastructure.mapper;


import com.g42.platform.gms.booking_management.domain.entity.BookingRequest;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequestDetail;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestDetailJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingDraffManagerMapper {
BookingRequest toDomain(BookingRequestJpa bookingRequestJpa );
List<BookingRequest> toDomain(List<BookingRequestJpa> bookingRequestJpas);
BookingRequestJpa toDomainJpa(BookingRequest bookingRequestJpa );
}
