package com.g42.platform.gms.service_ticket_management.api.dto.backfill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Giới hạn nhập bù của người đang đăng nhập — FE dùng để chặn ô chọn ngày. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BackfillPolicyDto {
    private int maxDaysBack;
    /** Mốc sớm nhất được chọn (00:00 của ngày now - maxDaysBack). */
    private LocalDateTime earliestAllowed;
    private boolean canReview;
}
