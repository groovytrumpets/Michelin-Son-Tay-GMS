package com.g42.platform.gms.service_ticket_management.api.dto.backfill;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ghi chú của quản lý khi duyệt (không bắt buộc) hoặc từ chối (bắt buộc). */
@Getter
@Setter
@NoArgsConstructor
public class BackfillReviewRequest {
    private String note;
}
