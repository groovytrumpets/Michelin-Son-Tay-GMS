package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "specification", schema = "michelin_garage")
public class SpecificationJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spec_id", nullable = false)
    private Integer specId;

    @NotNull
    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @NotNull
    @Column(name = "attribute_id", nullable = false)
    private Integer attribute;

    @Size(max = 100)
    @Column(name = "spec_value", length = 100)
    private String specValue;


}