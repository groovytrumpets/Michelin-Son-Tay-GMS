package com.g42.platform.gms.customerimport.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả một lần kiểm tra thử hoặc một lần ghi.
 *
 * Ở chế độ kiểm tra thử (dryRun = true) các con số là dự đoán và batchId để trống;
 * ở chế độ ghi, chúng là số thật đã ghi vào cơ sở dữ liệu.
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomerImportReport {

    private boolean dryRun;
    private Integer batchId;

    private int totalRows;
    private int skippedRows;

    private int customersCreated;
    private int customersMerged;
    private int vehiclesCreated;
    private int vehiclesLinked;
    private int visitsCreated;
    private int visitsDuplicate;
    private int visitItemsCreated;

    /** Khách bị đánh dấu không liên hệ nữa vì sổ cũ ghi "số chết", "chặn số". */
    private int customersMarkedDoNotContact;

    private List<ImportIssueDto> issues = new ArrayList<>();

    public void add(ImportIssueDto issue) {
        issues.add(issue);
    }

    public long getErrorCount() {
        return issues.stream().filter(i -> i.getSeverity() == ImportIssueDto.Severity.ERROR).count();
    }

    public long getWarningCount() {
        return issues.stream().filter(i -> i.getSeverity() == ImportIssueDto.Severity.WARNING).count();
    }
}
