package com.g42.platform.gms.bugreport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BugReportStatsDto {
    /** Tổng số phiếu khớp bộ lọc hiện tại. */
    private long total;
    private long newCount;
    private long inProgressCount;
    private long resolvedCount;
    /** Phiếu mức CRITICAL nhưng chưa đóng — cần xử lý ngay. */
    private long criticalOpenCount;
}
