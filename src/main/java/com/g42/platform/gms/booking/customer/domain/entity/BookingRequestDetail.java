package com.g42.platform.gms.booking.customer.domain.entity;

import lombok.Data;

@Data
public class BookingRequestDetail {
    private Integer requestDetailId;
    private Integer requestId;
    private Integer itemId;
}
