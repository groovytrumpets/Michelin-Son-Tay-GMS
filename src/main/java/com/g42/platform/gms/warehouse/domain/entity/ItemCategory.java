package com.g42.platform.gms.warehouse.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
public class ItemCategory {

    private Integer itemCategoryId;
    private String categoryCode;
    private String categoryName;
    private String categoryType;
    private Byte isActive;


}