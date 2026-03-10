package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.PressureUnit;
import com.g42.platform.gms.service_ticket_management.domain.enums.TirePosition;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Domain entity representing tire measurement data in a safety inspection.
 */
@Data
public class SafetyInspectionTire {
    
    private Integer tireId;
    private Integer inspectionId;
    private TirePosition tirePosition;
    private BigDecimal treadDepth;  // in millimeters
    private BigDecimal pressure;     // in PSI or BAR
    private PressureUnit pressureUnit;
}
