package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Một khách hàng tìm được trong popup "Chuyển phiếu sang khách khác", kèm các xe của khách. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReassignCandidate {
    private Integer customerId;
    private String customerCode;
    private String fullName;
    private String companyName;
    private String phone;
    private String email;
    private List<VehicleLine> vehicles;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleLine {
        private Integer vehicleId;
        private String licensePlate;
        private String brand;
        private String model;
    }
}
