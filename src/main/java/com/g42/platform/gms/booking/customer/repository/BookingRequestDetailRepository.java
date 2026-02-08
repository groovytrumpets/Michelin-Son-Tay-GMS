package com.g42.platform.gms.booking.customer.repository;

import com.g42.platform.gms.booking.customer.entity.BookingRequestDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRequestDetailRepository extends JpaRepository<BookingRequestDetail, Integer> {
    
    List<BookingRequestDetail> findByRequest_RequestId(Integer requestId);
    
    void deleteByRequest_RequestId(Integer requestId);
}
