package com.g42.platform.gms.booking.customer.infrastructure.jpa;

import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, Integer> {
    List<BookingJpaEntity> findByCustomer_CustomerIdOrderByScheduledDateDescScheduledTimeDesc(Integer customerId);
}
