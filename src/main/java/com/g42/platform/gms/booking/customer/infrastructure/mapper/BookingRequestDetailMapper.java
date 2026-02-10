package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.BookingRequestDetail;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestDetailJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class BookingRequestDetailMapper {
    
    public BookingRequestDetail toDomain(BookingRequestDetailJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        
        BookingRequestDetail domain = new BookingRequestDetail();
        domain.setRequestDetailId(jpa.getRequestDetailId());
        
        Integer requestId = null;
        if (jpa.getRequest() != null) {
            requestId = jpa.getRequest().getRequestId();
        }
        domain.setRequestId(requestId);
        
        Integer itemId = null;
        if (jpa.getItem() != null) {
            itemId = jpa.getItem().getItemId();
        }
        domain.setItemId(itemId);
        
        return domain;
    }
    
    public BookingRequestDetailJpaEntity toJpa(BookingRequestDetail domain) {
        if (domain == null) {
            return null;
        }
        
        BookingRequestDetailJpaEntity jpa = new BookingRequestDetailJpaEntity();
        jpa.setRequestDetailId(domain.getRequestDetailId());
        // Note: item and request will be set in repository implementation
        
        return jpa;
    }
}
