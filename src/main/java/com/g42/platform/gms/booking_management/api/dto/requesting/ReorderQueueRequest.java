package com.g42.platform.gms.booking_management.api.dto.requesting;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReorderQueueRequest {
    private List<QueueOrderItem> orders;
}
