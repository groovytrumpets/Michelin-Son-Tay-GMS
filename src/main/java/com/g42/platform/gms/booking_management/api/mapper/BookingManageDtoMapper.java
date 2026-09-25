package com.g42.platform.gms.booking_management.api.mapper;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedDetailResponse;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.domain.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingManageDtoMapper {
    @Mapping(target = "branchName",
            expression = "java(com.g42.platform.gms.branch.service.BranchDirectory.nameFor(booking.getBranchId()))")
    BookedRespond toBookedRespond(Booking booking);
    @Mapping(source = "services", target = "items")
    BookedDetailResponse toBookedDetailResponse(Booking booking);
}
