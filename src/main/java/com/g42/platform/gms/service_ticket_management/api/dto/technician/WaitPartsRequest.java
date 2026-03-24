package com.g42.platform.gms.service_ticket_management.api.dto.technician;

import lombok.Data;

@Data
public class WaitPartsRequest {
    private String reason; // lý do chờ phụ tùng (optional)
}
