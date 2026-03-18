package com.g42.platform.gms.service_ticket_management.api.dto.work_history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Response DTO for work history records.
 * Contains information about completed service tickets for a technician.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkHistoryResponse {
    
    private Integer serviceTicketId;
    private String ticketCode;
    
    // Ngày hoàn thành (yyyy-MM-dd)
    private LocalDate completedDate;
    
    // Thông tin xe
    private String licensePlate;
    private String vehicleBrand;
    private String vehicleModel;
    private Integer vehicleYear;
    
    // Thông tin khách hàng
    private String customerName;
    private String customerPhone;
    
    // Loại dịch vụ
    private String serviceType;
    
    // Ghi chú
    private String customerRequest;
    private String technicianNotes;
}
