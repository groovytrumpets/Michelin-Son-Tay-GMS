package com.g42.platform.gms.customerimport.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lần cuối khách đến xưởng và tổng số lần, gộp phiếu dịch vụ trong phần mềm với lịch
 * sử nhập từ sổ Excel cũ. Đây là con số để trả lời "khách này bao lâu chưa quay lại".
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerVisitStatsDto {

    private Integer customerId;
    private LocalDateTime lastVisitAt;
    private Integer daysSinceLastVisit;

    /** Tổng số lần đến, cộng cả hai nguồn. */
    private int visitCount;

    private int ticketVisitCount;
    private int legacyVisitCount;

    /** Lần cuối đến xưởng lấy từ nguồn nào: SYSTEM hay LEGACY. */
    private String lastVisitSource;
}
