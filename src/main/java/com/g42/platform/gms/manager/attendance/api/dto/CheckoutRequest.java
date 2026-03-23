package com.g42.platform.gms.manager.attendance.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CheckoutRequest {
    private LocalTime checkOutTime; // null = giờ hiện tại
    private String notes;
}
