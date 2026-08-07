package com.g42.platform.gms.vehicle.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleBrandDto {
    private Integer brandId;
    private String name;
    private String country;
    private String logoUrl;
    private Boolean active;
    /** Danh sách tên dòng xe, chỉ có khi client yêu cầu kèm model. */
    private List<String> models;
}
