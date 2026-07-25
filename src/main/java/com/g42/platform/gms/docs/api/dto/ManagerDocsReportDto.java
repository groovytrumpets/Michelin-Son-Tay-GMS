package com.g42.platform.gms.docs.api.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ManagerDocsReportDto {
    private Integer staffId;
    private String fullName;
    private String employeeNo;
    private String position;
    private String avatar;
    private long completedTopicsCount;
    private int totalTopics;
    private double completionPercentage;
    private LocalDateTime lastActiveAt;
}
