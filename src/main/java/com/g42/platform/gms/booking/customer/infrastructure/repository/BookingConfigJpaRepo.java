package com.g42.platform.gms.booking.customer.infrastructure.repository;

import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingConfigJpaRepo extends JpaRepository<BookingConfigJpa, Integer> {
}
