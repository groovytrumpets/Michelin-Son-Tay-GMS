package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.PressureUnit;
import com.g42.platform.gms.service_ticket_management.domain.enums.TirePosition;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * JPA entity for safety_inspection_tire table.
 */
@Entity
@Table(name = "safety_inspection_tire", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"inspection_id", "tire_position"}))
@Data
public class SafetyInspectionTireJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tire_id")
    private Integer tireId;
    
    @Column(name = "inspection_id", nullable = false)
    private Integer inspectionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tire_position", length = 20, nullable = false)
    private TirePosition tirePosition;
    
    @Column(name = "tread_depth", precision = 5, scale = 2)
    private BigDecimal treadDepth;
    
    @Column(name = "pressure", precision = 5, scale = 2)
    private BigDecimal pressure;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "pressure_unit", length = 10)
    private PressureUnit pressureUnit;

    @Column(name = "tire_specification", length = 50)
    private String tireSpecification;

    @Column(name = "recommended_tire_size", length = 50)
    private String recommendedTireSize;

    @Column(name = "recommended_pressure", precision = 5, scale = 2)
    private BigDecimal recommendedPressure;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_pressure_unit", length = 10)
    private PressureUnit recommendedPressureUnit;
}
