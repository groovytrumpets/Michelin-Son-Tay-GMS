package com.g42.platform.gms.service_ticket_management.api.dto.backfill;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Thống kê phiếu nhập bù theo nhân viên — ai hay nhập bù/bị từ chối nhiều. */
@Getter
@Setter
@NoArgsConstructor
public class BackfillStaffStatDto {
    private Integer staffId;
    private String staffName;
    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private BigDecimal approvedAmount = BigDecimal.ZERO;
}
