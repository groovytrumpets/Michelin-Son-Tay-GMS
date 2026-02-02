package com.g42.platform.gms.booking.repository;

import com.g42.platform.gms.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    // Sau này có thể thêm tìm kiếm theo ngày để check full lịch
}