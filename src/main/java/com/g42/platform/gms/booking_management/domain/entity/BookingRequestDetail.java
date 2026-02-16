package com.g42.platform.gms.booking_management.domain.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDetail {

    private Integer requestDetailId;
    private BookingRequest request;
    private CatalogItem item;
}
