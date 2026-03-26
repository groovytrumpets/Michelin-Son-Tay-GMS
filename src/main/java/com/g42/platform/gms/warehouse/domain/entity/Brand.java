package com.g42.platform.gms.warehouse.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "brand", schema = "michelin_garage")
public class Brand {
    private Integer id;
    private String brandName;
    private String logoUrl;
    private Byte isActive;


}