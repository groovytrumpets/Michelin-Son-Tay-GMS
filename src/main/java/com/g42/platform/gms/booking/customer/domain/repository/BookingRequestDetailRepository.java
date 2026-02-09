package com.g42.platform.gms.booking.customer.domain.repository;

import com.g42.platform.gms.booking.customer.domain.entity.BookingRequestDetail;

import java.util.List;

public interface BookingRequestDetailRepository {
    BookingRequestDetail save(BookingRequestDetail detail);
    List<BookingRequestDetail> saveAll(List<BookingRequestDetail> details);
    List<BookingRequestDetail> findByRequestId(Integer requestId);
    void deleteByRequestId(Integer requestId);
}
