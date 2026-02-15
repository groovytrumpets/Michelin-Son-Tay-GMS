package com.g42.platform.gms.booking_management.application.dto;

import java.time.LocalDateTime;

public record CustomerData(
        Integer id,
        String fullName,
        String phone,
        LocalDateTime firstBookingAt
) {
}
