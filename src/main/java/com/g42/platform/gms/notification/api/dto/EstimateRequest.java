package com.g42.platform.gms.notification.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class EstimateRequest {
    private String number;
    private String customerName;
    private List<String> productName;
    private String orderCode;
    private String garageLocation;
    private String totalPrice;
}
