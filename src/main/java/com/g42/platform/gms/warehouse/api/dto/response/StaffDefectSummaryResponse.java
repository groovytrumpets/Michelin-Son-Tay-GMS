package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.DefectCause;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tổng hợp lỗi hàng theo nhân viên — dùng cho báo cáo KPI penalty.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffDefectSummaryResponse {
    /** ID nhân viên chịu trách nhiệm */
    private Integer staffId;
    /** Tên nhân viên */
    private String staffName;
    /** Nguyên nhân lỗi */
    private DefectCause defectCause;
    /** Số lần gây lỗi (số phiếu hoàn CONFIRMED) */
    private Long defectCount;
    /** Tổng số lượng hàng lỗi */
    private Long defectQuantity;
}
