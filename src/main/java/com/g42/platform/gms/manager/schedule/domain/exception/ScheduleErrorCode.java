package com.g42.platform.gms.manager.schedule.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode {
    SHIFT_NOT_FOUND("SHIFT_NOT_FOUND", "Không tìm thấy ca làm việc");

    private final String code;
    private final String message;
}
