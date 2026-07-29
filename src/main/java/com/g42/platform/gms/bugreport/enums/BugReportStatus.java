package com.g42.platform.gms.bugreport.enums;

/** Vòng đời xử lý một phiếu báo lỗi phần mềm. */
public enum BugReportStatus {
    NEW,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED
}
