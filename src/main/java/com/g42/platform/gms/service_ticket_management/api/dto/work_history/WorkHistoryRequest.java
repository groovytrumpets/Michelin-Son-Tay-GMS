package com.g42.platform.gms.service_ticket_management.api.dto.work_history;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for work history queries.
 * Contains date range and optional license plate filter.
 */
@Data
public class WorkHistoryRequest {
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    @NotNull(message = "End date is required")
    private LocalDate endDate;
    
    private String licensePlate; // Optional
}
