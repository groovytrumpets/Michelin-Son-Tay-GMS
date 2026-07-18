package com.g42.platform.gms.systemlog.service;

import com.g42.platform.gms.systemlog.dto.SystemLogDto;
import com.g42.platform.gms.systemlog.dto.SystemLogStatsDto;
import com.g42.platform.gms.systemlog.entity.SystemLogJpa;
import com.g42.platform.gms.systemlog.repository.SystemLogJpaRepo;
import com.g42.platform.gms.systemlog.specification.SystemLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemLogQueryService {

    private static final List<String> DATA_CHANGE_ACTIONS =
            List.of("CREATE", "UPDATE", "DELETE", "PERMISSION");
    private static final int EXPORT_MAX_ROWS = 10000;

    private final SystemLogJpaRepo systemLogJpaRepo;

    public Page<SystemLogDto> search(int page, int size,
                                     LocalDateTime startDate, LocalDateTime endDate,
                                     String role, String action, String severity,
                                     String module, String search) {
        Specification<SystemLogJpa> spec = SystemLogSpecification
                .filter(startDate, endDate, role, action, severity, module, search);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "logId"));
        return systemLogJpaRepo.findAll(spec, pageable).map(this::toDto);
    }

    public SystemLogStatsDto stats(LocalDateTime startDate, LocalDateTime endDate,
                                   String role, String action, String severity,
                                   String module, String search) {
        Specification<SystemLogJpa> spec = SystemLogSpecification
                .filter(startDate, endDate, role, action, severity, module, search);
        long total = systemLogJpaRepo.count(spec);
        long loginFailed = systemLogJpaRepo.count(SystemLogSpecification.withAction(spec, "LOGIN_FAILED"));
        long dataChange = systemLogJpaRepo.count(SystemLogSpecification.withActionIn(spec, DATA_CHANGE_ACTIONS));
        return new SystemLogStatsDto(total, loginFailed, dataChange);
    }

    /** Xuất CSV (UTF-8 BOM để Excel hiển thị đúng tiếng Việt), tối đa EXPORT_MAX_ROWS dòng. */
    public byte[] exportCsv(LocalDateTime startDate, LocalDateTime endDate,
                            String role, String action, String severity,
                            String module, String search) {
        Specification<SystemLogJpa> spec = SystemLogSpecification
                .filter(startDate, endDate, role, action, severity, module, search);
        List<SystemLogJpa> rows = systemLogJpaRepo.findAll(spec,
                PageRequest.of(0, EXPORT_MAX_ROWS, Sort.by(Sort.Direction.DESC, "createdAt", "logId"))).getContent();

        StringBuilder sb = new StringBuilder("﻿");
        sb.append("ID,Thời gian,Nhân viên,Vai trò,Hành động,Phân hệ,Mức độ,Mô tả,Đối tượng,Mã đối tượng,IP,Trình duyệt\n");
        for (SystemLogJpa r : rows) {
            sb.append(r.getLogId()).append(',')
              .append(csv(r.getCreatedAt() == null ? "" : r.getCreatedAt().toString())).append(',')
              .append(csv(r.getActorName())).append(',')
              .append(csv(r.getActorRole())).append(',')
              .append(csv(r.getAction())).append(',')
              .append(csv(r.getModule())).append(',')
              .append(csv(r.getSeverity())).append(',')
              .append(csv(r.getDescription())).append(',')
              .append(csv(r.getTargetType())).append(',')
              .append(csv(r.getTargetId())).append(',')
              .append(csv(r.getIpAddress())).append(',')
              .append(csv(r.getUserAgent())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n"))
                ? "\"" + escaped + "\"" : escaped;
    }

    private SystemLogDto toDto(SystemLogJpa r) {
        return SystemLogDto.builder()
                .logId(r.getLogId())
                .actorStaffId(r.getActorStaffId())
                .actorName(r.getActorName())
                .actorRole(r.getActorRole())
                .action(r.getAction())
                .module(r.getModule())
                .severity(r.getSeverity())
                .description(r.getDescription())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .ipAddress(r.getIpAddress())
                .userAgent(r.getUserAgent())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
