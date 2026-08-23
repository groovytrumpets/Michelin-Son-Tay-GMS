package com.g42.platform.gms.customerimport.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** Kết quả gom nhóm theo khách của một nguồn lịch sử. Chỉ dùng nội bộ giữa repo và service. */
@Getter
@AllArgsConstructor
public class VisitAggregateDto {
    private final Integer customerId;
    private final LocalDateTime lastVisitAt;
    private final Long visitCount;
}
