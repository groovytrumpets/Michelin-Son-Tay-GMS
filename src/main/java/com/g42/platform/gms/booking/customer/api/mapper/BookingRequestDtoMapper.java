package com.g42.platform.gms.booking.customer.api.mapper;

import com.g42.platform.gms.booking.customer.api.dto.BookingRequestResponse;
import com.g42.platform.gms.booking.customer.domain.entity.BookingRequest;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public class BookingRequestDtoMapper {

    public BookingRequestResponse toResponse(BookingRequest domain) {
        if (domain == null) {
            return null;
        }
        
        BookingRequestResponse response = new BookingRequestResponse();
        response.setRequestId(domain.getRequestId());
        response.setPhone(domain.getPhone());
        response.setFullName(domain.getFullName());
        response.setScheduledDate(domain.getScheduledDate());
        response.setScheduledTime(domain.getScheduledTime());
        response.setDescription(domain.getDescription());
        
        String status = null;
        if (domain.getStatus() != null) {
            status = domain.getStatus().name();
        }
        response.setStatus(status);
        response.setCreatedAt(domain.getCreatedAt());
        response.setExpiresAt(domain.getExpiresAt());
        
        return response;
    }
}
