package com.g42.platform.gms.vehicle.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating a new vehicle.
 */
@Data
public class CreateVehicleRequest {

    @NotBlank
    private String licensePlate;

    private String brand;

    private String model;

    private Integer manufactureYear;

    @NotNull
    private Integer customerId;
}
