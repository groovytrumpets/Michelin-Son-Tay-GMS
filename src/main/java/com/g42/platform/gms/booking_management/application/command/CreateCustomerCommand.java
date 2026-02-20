package com.g42.platform.gms.booking_management.application.command;

import java.time.LocalDateTime;

public record CreateCustomerCommand (
        String fullName,
        String phone,
        LocalDateTime firstBookingAt
) {
}
