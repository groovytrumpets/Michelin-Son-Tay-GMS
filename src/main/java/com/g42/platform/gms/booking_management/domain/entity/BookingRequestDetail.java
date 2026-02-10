package com.g42.platform.gms.booking_management.domain.entity;

import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDetail {

    private Integer requestDetailId;
    private BookingRequest request;
    private CatalogItemJpa item;
}
