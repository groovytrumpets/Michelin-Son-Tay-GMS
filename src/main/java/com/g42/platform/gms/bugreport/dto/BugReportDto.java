package com.g42.platform.gms.bugreport.dto;

import com.g42.platform.gms.bugreport.enums.BugReportCategory;
import com.g42.platform.gms.bugreport.enums.BugReportSeverity;
import com.g42.platform.gms.bugreport.enums.BugReportStatus;
import com.g42.platform.gms.bugreport.enums.ReporterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugReportDto {
    private Long reportId;
    private String title;
    private String description;
    private BugReportCategory category;
    private BugReportSeverity severity;
    private String module;
    private BugReportStatus status;
    private ReporterType reporterType;
    private Integer reporterStaffId;
    private Integer reporterCustomerId;
    private String reporterName;
    private String reporterRole;
    private String reporterContact;
    private String pageUrl;
    private String userAgent;
    private String screenSize;
    private String appVersion;
    private String ipAddress;
    private Integer assignedStaffId;
    private String assignedStaffName;
    private String resolutionNote;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> attachmentUrls;
}
