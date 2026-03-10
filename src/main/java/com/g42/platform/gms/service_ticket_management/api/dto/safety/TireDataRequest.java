package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import com.g42.platform.gms.service_ticket_management.domain.enums.PressureUnit;
import com.g42.platform.gms.service_ticket_management.domain.enums.TirePosition;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TireDataRequest {
    
    private TirePosition tirePosition;
    private BigDecimal treadDepth;
    private BigDecimal pressure;
    private PressureUnit pressureUnit;
}