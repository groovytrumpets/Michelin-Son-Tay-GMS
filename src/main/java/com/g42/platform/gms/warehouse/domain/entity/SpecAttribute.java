package com.g42.platform.gms.warehouse.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpecAttribute {
    private Integer attributeId;
    private String attributeCode;
    private String displayName;
    private String unit;


}