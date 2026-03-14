package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO để update service ticket (receptionist).
 */
@Data
public class UpdateServiceTicketRequest {
    
    /**
     * Yêu cầu của khách hàng (customer request).
     * Có thể update để ghi chú thêm thông tin.
     */
    @Size(max = 1000, message = "Yêu cầu khách hàng không được vượt quá 1000 ký tự")
    private String customerRequest;
    
    /**
     * Danh sách catalog item IDs đã chọn.
     * Update vào booking thông qua bảng booking_details.
     */
    @NotNull(message = "Danh sách dịch vụ không được null")
    private List<Integer> catalogItemIds;
}
