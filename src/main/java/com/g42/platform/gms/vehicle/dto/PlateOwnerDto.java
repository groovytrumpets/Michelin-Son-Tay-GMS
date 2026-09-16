package com.g42.platform.gms.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một hồ sơ khách đang gắn biển số này. Một biển số có thể có nhiều chủ (vợ chồng, gia đình,
 * công ty dùng chung xe) — xem changeset 039.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlateOwnerDto {
    private Integer vehicleId;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer manufactureYear;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String customerCode;
}
