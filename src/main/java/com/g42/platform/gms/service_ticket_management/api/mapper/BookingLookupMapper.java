package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.service_ticket_management.api.dto.checkin.BookingLookupResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper cho Booking Lookup Response trong quy trình Check-in.
 */
@Mapper(componentModel = "spring")
public interface BookingLookupMapper {
    
    /**
     * Map Booking và Customer sang BookingLookupResponse.
     */
    @Mapping(target = "bookingId", source = "booking.bookingId")
    @Mapping(target = "bookingCode", source = "booking.bookingCode")
    @Mapping(target = "scheduledDate", source = "booking.scheduledDate")
    @Mapping(target = "scheduledTime", source = "booking.scheduledTime")
    @Mapping(target = "serviceCategory", source = "booking.serviceCategory")
    @Mapping(target = "description", source = "booking.description")
    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "customerEmail", source = "customer.email")
    @Mapping(target = "services", ignore = true)
    BookingLookupResponse toResponse(Booking booking, CustomerProfile customer);
    
    /**
     * Map CatalogItem sang ServiceInfo.
     */
    @Mapping(target = "serviceId", source = "itemId")
    @Mapping(target = "serviceName", source = "itemName")
    @Mapping(target = "category", source = "itemType")
    BookingLookupResponse.ServiceInfo toServiceInfo(CatalogItemJpaEntity catalogItem);
    
    /**
     * Map list of CatalogItems sang list of ServiceInfo.
     */
    List<BookingLookupResponse.ServiceInfo> toServiceInfoList(List<CatalogItemJpaEntity> catalogItems);
}
