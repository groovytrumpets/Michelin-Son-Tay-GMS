package com.g42.platform.gms.bugreport.service;

import com.g42.platform.gms.bugreport.dto.BugReportDto;
import com.g42.platform.gms.bugreport.entity.BugReportAttachmentJpa;
import com.g42.platform.gms.bugreport.entity.BugReportJpa;

import java.util.List;

public final class BugReportMapper {

    private BugReportMapper() {
    }

    public static BugReportDto toDto(BugReportJpa entity) {
        List<String> urls = entity.getAttachments() == null
                ? List.of()
                : entity.getAttachments().stream().map(BugReportAttachmentJpa::getImageUrl).toList();

        return BugReportDto.builder()
                .reportId(entity.getReportId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .severity(entity.getSeverity())
                .module(entity.getModule())
                .status(entity.getStatus())
                .reporterType(entity.getReporterType())
                .reporterStaffId(entity.getReporterStaffId())
                .reporterCustomerId(entity.getReporterCustomerId())
                .reporterName(entity.getReporterName())
                .reporterRole(entity.getReporterRole())
                .reporterContact(entity.getReporterContact())
                .pageUrl(entity.getPageUrl())
                .userAgent(entity.getUserAgent())
                .screenSize(entity.getScreenSize())
                .appVersion(entity.getAppVersion())
                .ipAddress(entity.getIpAddress())
                .assignedStaffId(entity.getAssignedStaffId())
                .assignedStaffName(entity.getAssignedStaffName())
                .resolutionNote(entity.getResolutionNote())
                .resolvedAt(entity.getResolvedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .attachmentUrls(urls)
                .build();
    }
}
