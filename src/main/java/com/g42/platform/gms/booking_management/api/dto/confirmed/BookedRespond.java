package com.g42.platform.gms.booking_management.api.dto.confirmed;

import com.g42.platform.gms.booking_management.api.dto.CustomerDto;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookedRespond {
    private Integer bookingId;
    private String bookingCode;
    private CustomerDto customer;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private BookingEnum status;
    private String description;
    private Boolean isGuest;
    private LocalDateTime createdAt;
    private Integer estimateTime;
    private Integer queueOrder;
}
