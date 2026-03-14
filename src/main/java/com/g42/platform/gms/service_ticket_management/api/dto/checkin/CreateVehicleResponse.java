package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import lombok.Data;

/**
 * Response DTO after creating a new vehicle.
 * Returns the created vehicle information.
 */
@Data
public class CreateVehicleResponse {
    
    private Integer vehicleId;
    private String licensePlate;
    private String make;
    private String model;
    private Integer year;
    private Integer customerId;
    private String message;
}
