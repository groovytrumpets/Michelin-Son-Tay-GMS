package com.g42.platform.gms.estimation.api.dto;

import com.g42.platform.gms.estimation.domain.enums.CommissionPartyType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Một dòng hoa hồng của phiếu báo giá.
 * Gửi lên chỉ cần partyType + (ratePercent hoặc amount); baseAmount và amount
 * được tính lại ở server nên giá trị client gửi chỉ mang tính tham khảo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommissionAllocationDto {
    private Integer allocationId;
    private Integer estimateItemId;
    private CommissionPartyType partyType;
    private Integer partnerId;
    /** Tên bên nhận, tra từ danh bạ đối tác để hiển thị. */
    private String partnerName;
    private Integer staffId;
    private BigDecimal baseAmount;
    private BigDecimal ratePercent;
    private BigDecimal amount;
    private String note;
}
