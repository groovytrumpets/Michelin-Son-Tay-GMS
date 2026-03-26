package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "spec_attribute", schema = "michelin_garage")
public class SpecAttributeJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attribute_id", nullable = false)
    private Integer attributeId;

    @Size(max = 45)
    @Column(name = "attribute_code", length = 45)
    private String attributeCode;

    @Size(max = 45)
    @Column(name = "display_name", length = 45)
    private String displayName;

    @Size(max = 45)
    @Column(name = "unit", length = 45)
    private String unit;


}