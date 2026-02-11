package com.g42.platform.gms.booking_management.infrastructure.repository;

import com.g42.platform.gms.booking_management.infrastructure.entity.BookingDetailJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingMDetailJpaRepo extends JpaRepository<BookingDetailJpa, Integer> {
}
