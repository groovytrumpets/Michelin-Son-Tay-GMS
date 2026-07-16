package com.g42.platform.gms.service_ticket_management.api.dto.parts_sale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request tạo phiếu bán linh kiện (PARTS_SALE) từ một báo giá DRAFT.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartsSaleCreateDto {
    private Integer customerId;
    private Integer estimateId;
    private String note;
    private Integer bookingId;
}
