package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.BookingRequest;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestDetailJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestJpaEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class BookingRequestMapper {
    
    public BookingRequest toDomain(BookingRequestJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        
        BookingRequest domain = new BookingRequest();
        domain.setRequestId(jpa.getRequestId());
        domain.setPhone(jpa.getPhone());
        domain.setFullName(jpa.getFullName());
        domain.setScheduledDate(jpa.getScheduledDate());
        domain.setScheduledTime(jpa.getScheduledTime());
        domain.setDescription(jpa.getDescription());
        domain.setServiceCategory(jpa.getServiceCategory());
        domain.setStatus(jpa.getStatus());
        domain.setIsGuest(jpa.getIsGuest());
        
        Integer customerId = null;
        if (jpa.getCustomer() != null) {
            customerId = jpa.getCustomer().getCustomerId();
        }
        domain.setCustomerId(customerId);
        
        Integer confirmedBy = null;
        if (jpa.getConfirmedBy() != null) {
            confirmedBy = (int) jpa.getConfirmedBy().getStaffId();
        }
        domain.setConfirmedBy(confirmedBy);
        domain.setConfirmedAt(jpa.getConfirmedAt());
        domain.setRejectionReason(jpa.getRejectionReason());
        domain.setCreatedAt(jpa.getCreatedAt());
        domain.setExpiresAt(jpa.getExpiresAt());
        domain.setClientIp(jpa.getClientIp());
        
        if (jpa.getDetails() != null) {
            domain.setServiceIds(jpa.getDetails().stream()
                .map((BookingRequestDetailJpaEntity detail) -> {
                    Integer itemId = null;
                    if (detail.getItem() != null) {
                        itemId = detail.getItem().getItemId();
                    }
                    return itemId;
                })
                .filter(id -> id != null)
                .collect(Collectors.toList()));
        }
        
        return domain;
    }
    
    public BookingRequestJpaEntity toJpa(BookingRequest domain) {
        if (domain == null) {
            return null;
        }
        
        BookingRequestJpaEntity jpa = new BookingRequestJpaEntity();
        jpa.setRequestId(domain.getRequestId());
        jpa.setPhone(domain.getPhone());
        jpa.setFullName(domain.getFullName());
        jpa.setScheduledDate(domain.getScheduledDate());
        jpa.setScheduledTime(domain.getScheduledTime());
        jpa.setDescription(domain.getDescription());
        jpa.setServiceCategory(domain.getServiceCategory());
        jpa.setStatus(domain.getStatus());
        jpa.setIsGuest(domain.getIsGuest());
        jpa.setConfirmedAt(domain.getConfirmedAt());
        jpa.setRejectionReason(domain.getRejectionReason());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setExpiresAt(domain.getExpiresAt());
        jpa.setClientIp(domain.getClientIp());
        
        return jpa;
    }
}
