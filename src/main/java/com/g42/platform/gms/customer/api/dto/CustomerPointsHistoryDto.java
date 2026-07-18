package com.g42.platform.gms.customer.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPointsHistoryDto {
    private Integer historyId;
    private Integer pointsDelta;
    private String reason;
    private Integer refBookingId;
    private Long amountSpent;
    private LocalDateTime createdAt;
}
