package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerBookingRequest extends BaseBookingRequest {
    // Tất cả validation đã có trong BaseBookingRequest
    // Không cần thêm field nào
}
