package com.g42.platform.gms.bugreport.service;

import com.g42.platform.gms.bugreport.dto.BugReportDto;
import com.g42.platform.gms.bugreport.dto.BugReportStatsDto;
import com.g42.platform.gms.bugreport.dto.BugReportUpdateRequest;
import com.g42.platform.gms.bugreport.entity.BugReportJpa;
import com.g42.platform.gms.bugreport.enums.BugReportSeverity;
import com.g42.platform.gms.bugreport.enums.BugReportStatus;
import com.g42.platform.gms.bugreport.repository.BugReportJpaRepo;
import com.g42.platform.gms.bugreport.exception.BugReportException;
import com.g42.platform.gms.bugreport.specification.BugReportSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/** Truy vấn và xử lý phiếu báo lỗi — chỉ phục vụ màn hình quản trị. */
@Service
@RequiredArgsConstructor
public class BugReportQueryService {

    private static final int EXPORT_MAX_ROWS = 10000;

    private final BugReportJpaRepo bugReportJpaRepo;

    public Page<BugReportDto> search(int page, int size,
                                     LocalDateTime startDate, LocalDateTime endDate,
                                     String status, String severity, String category,
                                     String module, String reporterType, String search) {
        Specification<BugReportJpa> spec = BugReportSpecification
                .filter(startDate, endDate, status, severity, category, module, reporterType, search);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "reportId"));
        return bugReportJpaRepo.findAll(spec, pageable).map(BugReportMapper::toDto);
    }

    public BugReportDto getById(Long reportId) {
        return BugReportMapper.toDto(findOrThrow(reportId));
    }

    public BugReportStatsDto stats(LocalDateTime startDate, LocalDateTime endDate,
                                   String status, String severity, String category,
                                   String module, String reporterType, String search) {
        Specification<BugReportJpa> spec = BugReportSpecification
                .filter(startDate, endDate, status, severity, category, module, reporterType, search);
        return new BugReportStatsDto(
                bugReportJpaRepo.count(spec),
                bugReportJpaRepo.count(BugReportSpecification.withStatus(spec, BugReportStatus.NEW)),
                bugReportJpaRepo.count(BugReportSpecification.withStatus(spec, BugReportStatus.IN_PROGRESS)),
                bugReportJpaRepo.count(BugReportSpecification.withStatus(spec, BugReportStatus.RESOLVED)),
                bugReportJpaRepo.count(BugReportSpecification.withSeverity(
                        BugReportSpecification.withStatusIn(spec, BugReportSpecification.OPEN_STATUSES),
                        BugReportSeverity.CRITICAL)));
    }

    @Transactional
    public BugReportDto update(Long reportId, BugReportUpdateRequest request) {
        BugReportJpa entity = findOrThrow(reportId);

        entity.setStatus(request.getStatus());
        if (request.getSeverity() != null) {
            entity.setSeverity(request.getSeverity());
        }
        entity.setAssignedStaffId(request.getAssignedStaffId());
        entity.setAssignedStaffName(request.getAssignedStaffName());
        entity.setResolutionNote(request.getResolutionNote());
        entity.setUpdatedAt(LocalDateTime.now());

        boolean closed = request.getStatus() == BugReportStatus.RESOLVED
                || request.getStatus() == BugReportStatus.REJECTED;
        // Mốc đóng phiếu chỉ ghi lần đầu; mở lại phiếu thì xoá để tính lại thời gian xử lý.
        if (closed && entity.getResolvedAt() == null) {
            entity.setResolvedAt(LocalDateTime.now());
        } else if (!closed) {
            entity.setResolvedAt(null);
        }

        return BugReportMapper.toDto(bugReportJpaRepo.save(entity));
    }

    /** Xuất CSV (UTF-8 BOM để Excel hiển thị đúng tiếng Việt), tối đa EXPORT_MAX_ROWS dòng. */
    public byte[] exportCsv(LocalDateTime startDate, LocalDateTime endDate,
                            String status, String severity, String category,
                            String module, String reporterType, String search) {
        Specification<BugReportJpa> spec = BugReportSpecification
                .filter(startDate, endDate, status, severity, category, module, reporterType, search);
        List<BugReportJpa> rows = bugReportJpaRepo.findAll(spec,
                PageRequest.of(0, EXPORT_MAX_ROWS, Sort.by(Sort.Direction.DESC, "createdAt", "reportId"))).getContent();

        StringBuilder sb = new StringBuilder("﻿");
        sb.append("ID,Thời gian,Tiêu đề,Mô tả,Loại,Mức độ,Phân hệ,Trạng thái,Người gửi,Vai trò,Liên hệ,Trang phát sinh,Người xử lý,Ghi chú xử lý,Thời điểm đóng\n");
        for (BugReportJpa r : rows) {
            sb.append(r.getReportId()).append(',')
              .append(csv(r.getCreatedAt() == null ? "" : r.getCreatedAt().toString())).append(',')
              .append(csv(r.getTitle())).append(',')
              .append(csv(r.getDescription())).append(',')
              .append(csv(r.getCategory() == null ? "" : r.getCategory().name())).append(',')
              .append(csv(r.getSeverity() == null ? "" : r.getSeverity().name())).append(',')
              .append(csv(r.getModule())).append(',')
              .append(csv(r.getStatus() == null ? "" : r.getStatus().name())).append(',')
              .append(csv(r.getReporterName())).append(',')
              .append(csv(r.getReporterRole())).append(',')
              .append(csv(r.getReporterContact())).append(',')
              .append(csv(r.getPageUrl())).append(',')
              .append(csv(r.getAssignedStaffName())).append(',')
              .append(csv(r.getResolutionNote())).append(',')
              .append(csv(r.getResolvedAt() == null ? "" : r.getResolvedAt().toString())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private BugReportJpa findOrThrow(Long reportId) {
        return bugReportJpaRepo.findById(reportId)
                .orElseThrow(() -> BugReportException.notFound(reportId));
    }

    private static String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n"))
                ? "\"" + escaped + "\"" : escaped;
    }
}
