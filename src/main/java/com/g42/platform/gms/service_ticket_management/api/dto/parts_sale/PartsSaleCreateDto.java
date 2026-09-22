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
    /**
     * Phieu dang giu hang da tao truoc do (neu co). FE gui len khi luu lai bao gia
     * da sua de backend dung lai dung phieu cu thay vi tao phieu moi.
     */
    private Integer serviceTicketId;

    /**
     * Ban cho khach le vang lai: customerId phai la ho so dung chung "Khach le"
     * (WalkInCustomerService), con ten/SDT/dia chi that duoc luu thang len phieu.
     * Ten khach la bat buoc, hai truong con lai khong.
     */
    private Boolean walkIn;
    private String walkInName;
    private String walkInPhone;
    private String walkInAddress;
}
