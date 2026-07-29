package com.g42.platform.gms.bugreport.dto;

import com.g42.platform.gms.bugreport.enums.BugReportSeverity;
import com.g42.platform.gms.bugreport.enums.BugReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin cập nhật tiến độ xử lý một phiếu báo lỗi. */
@Data
public class BugReportUpdateRequest {

    @NotNull(message = "Vui lòng chọn trạng thái xử lý")
    private BugReportStatus status;

    /** Cho phép admin đánh giá lại mức độ so với người báo. Bỏ trống = giữ nguyên. */
    private BugReportSeverity severity;

    private Integer assignedStaffId;

    @Size(max = 150)
    private String assignedStaffName;

    @Size(max = 1000, message = "Ghi chú xử lý tối đa 1000 ký tự")
    private String resolutionNote;
}
