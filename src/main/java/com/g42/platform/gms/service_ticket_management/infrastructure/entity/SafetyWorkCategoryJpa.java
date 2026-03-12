package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "work_category")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SafetyWorkCategoryJpa {
    
    @Id
    @Column(name = "idwork_category")
    private Integer id;
    
    @Column(name = "category_code")
    private String categoryCode;
    
    @Column(name = "category_name")
    private String categoryName;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(name = "is_active")
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    private Boolean isActive;
    
    @Column(name = "is_default")
    @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
    private Boolean isDefault;

}