package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import lombok.Data;

/**
 * Response DTO containing vehicle information after selection or creation.
 */
@Data
public class VehicleResponse {
    
    private Integer vehicleId;
    private String licensePlate;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private Integer customerId;
    private String licensePlatePhotoUrl;
    private Boolean isNewVehicle;  // true if just created
    private String ticketCode;  // Service ticket code (ST_XXXXXX)
}
