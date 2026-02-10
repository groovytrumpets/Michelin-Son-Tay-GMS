package com.g42.platform.gms.booking.customer.api.mapper;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.api.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Mapper(componentModel = "spring")
public class BookingDtoMapper {
    
    private final CustomerProfileRepository customerRepository;
    
    public BookingResponse toResponse(Booking domain) {
        if (domain == null) {
            return null;
        }
        
        BookingResponse response = new BookingResponse();
        response.setBookingId(domain.getBookingId());
        response.setCustomerId(domain.getCustomerId());
        response.setScheduledDate(domain.getScheduledDate());
        response.setScheduledTime(domain.getScheduledTime());
        response.setDescription(domain.getDescription());
        
        String status = null;
        if (domain.getStatus() != null) {
            status = domain.getStatus().name();
        }
        response.setStatus(status);
        response.setIsGuest(domain.getIsGuest());
        response.setServiceIds(domain.getServiceIds());
        response.setVehicleId(domain.getVehicleId());
        
        if (domain.getCustomerId() != null) {
            customerRepository.findById(domain.getCustomerId()).ifPresent(customer -> {
                response.setCustomerName(customer.getFullName());
                response.setPhone(customer.getPhone());
            });
        }
        
        return response;
    }
}
