package com.g42.platform.gms.customerimport.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một vấn đề phát hiện ở một dòng cụ thể. Luôn kèm số dòng và tên cột để người
 * dùng mở file gốc là tìm thấy ngay chỗ cần sửa.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportIssueDto {

    public enum Severity {
        /** Dòng không ghi được, sẽ bị bỏ qua. */
        ERROR,
        /** Vẫn ghi được nhưng dữ liệu thiếu hoặc đáng ngờ. */
        WARNING
    }

    private Integer sourceRowNo;
    private String field;
    private Severity severity;
    private String message;

    public static ImportIssueDto error(Integer rowNo, String field, String message) {
        return new ImportIssueDto(rowNo, field, Severity.ERROR, message);
    }

    public static ImportIssueDto warning(Integer rowNo, String field, String message) {
        return new ImportIssueDto(rowNo, field, Severity.WARNING, message);
    }
}
