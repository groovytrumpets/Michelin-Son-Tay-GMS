package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;

@Data
public class ServiceItemDto {
    private Integer itemId;
    private String itemName;
    private String itemType;
    private Double estimatedPrice;
    private Integer estimateTime;
}
