package com.g42.platform.gms.vehicle.api.dto;

import lombok.Data;

/**
 * Request DTO for updating vehicle info.
 */
@Data
public class UpdateVehicleRequest {

    private String brand;
    private String model;
    private Integer manufactureYear;
}
