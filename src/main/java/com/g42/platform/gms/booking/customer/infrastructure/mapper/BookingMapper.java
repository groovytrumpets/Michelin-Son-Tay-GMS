package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class BookingMapper {
    
    public Booking toDomain(BookingJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        
        Booking domain = new Booking();
        domain.setBookingId(jpa.getBookingId());
        
        Integer customerId = null;
        if (jpa.getCustomer() != null) {
            customerId = jpa.getCustomer().getCustomerId();
        }
        domain.setCustomerId(customerId);
        
        Integer vehicleId = null;
        if (jpa.getVehicle() != null) {
            vehicleId = jpa.getVehicle().getVehicleId();
        }
        domain.setVehicleId(vehicleId);
        domain.setScheduledDate(jpa.getScheduledDate());
        domain.setScheduledTime(jpa.getScheduledTime());
        domain.setServiceCategory(jpa.getServiceCategory());
        domain.setStatus(jpa.getStatus());
        domain.setDescription(jpa.getDescription());
        domain.setIsGuest(jpa.getIsGuest());
        domain.setCreatedAt(jpa.getCreatedAt());
        
        if (jpa.getServices() != null) {
            domain.setServiceIds(jpa.getServices().stream()
                .map(service -> service.getItemId())
                .filter(id -> id != null)
                .collect(Collectors.toList()));
        }
        
        return domain;
    }
    
    public BookingJpaEntity toJpa(Booking domain) {
        if (domain == null) {
            return null;
        }
        
        BookingJpaEntity jpa = new BookingJpaEntity();
        jpa.setBookingId(domain.getBookingId());
        jpa.setScheduledDate(domain.getScheduledDate());
        jpa.setScheduledTime(domain.getScheduledTime());
        jpa.setServiceCategory(domain.getServiceCategory());
        jpa.setStatus(domain.getStatus());
        jpa.setDescription(domain.getDescription());
        jpa.setIsGuest(domain.getIsGuest());
        jpa.setCreatedAt(domain.getCreatedAt());
        
        return jpa;
    }
}
