package com.g42.platform.gms.booking.customer.infrastructure.jpa;

import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestDetailJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRequestDetailJpaRepository extends JpaRepository<BookingRequestDetailJpaEntity, Integer> {
    
    List<BookingRequestDetailJpaEntity> findByRequest_RequestId(Integer requestId);
    
    void deleteByRequest_RequestId(Integer requestId);
}
