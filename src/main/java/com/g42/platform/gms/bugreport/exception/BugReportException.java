package com.g42.platform.gms.bugreport.exception;

import lombok.Getter;

@Getter
public class BugReportException extends RuntimeException {

    private final String code;

    public BugReportException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static BugReportException notFound(Long reportId) {
        return new BugReportException("BUG_REPORT_NOT_FOUND", "Không tìm thấy phiếu báo lỗi #" + reportId);
    }
}
