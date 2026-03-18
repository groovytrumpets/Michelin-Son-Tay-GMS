package com.g42.platform.gms.booking_management.api.dto.requesting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueueOrderItem {
    private Integer bookingId;
    private Integer queueOrder;
}
