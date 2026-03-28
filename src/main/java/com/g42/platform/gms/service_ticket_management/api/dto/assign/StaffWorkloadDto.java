package com.g42.platform.gms.service_ticket_management.api.dto.assign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho workload của staff - hiển thị số lượng công việc đang làm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffWorkloadDto {
    private Integer staffId;
    private String fullName;
    private String phone;
    private List<String> roles;
    
    // Workload counters
    private Integer activeAssignments;      // Số assignment đang ACTIVE
    private Integer pendingAssignments;     // Số assignment đang PENDING
    private Integer totalWorkload;          // Tổng workload (ACTIVE + PENDING)
    
    // Availability status
    private Boolean isAvailable;            // Có thể assign thêm không
    private String availabilityReason;      // Lý do không available (nếu có)
    
    // Current tickets
    private List<WorkloadTicketDto> currentTickets;
}