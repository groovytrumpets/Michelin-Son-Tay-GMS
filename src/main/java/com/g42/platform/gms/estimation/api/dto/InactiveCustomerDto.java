package com.g42.platform.gms.estimation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InactiveCustomerDto {
    private Integer customerId;
    private String fullName;
    private String phone;
    private Integer serviceTicketId;
    private Integer vehicleId;
    private String licensePlate;
    private LocalDateTime lastVisitDate;
}
