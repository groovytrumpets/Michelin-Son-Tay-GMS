package com.g42.platform.gms.systemlog.service;

import com.g42.platform.gms.systemlog.dto.SystemLogDto;
import com.g42.platform.gms.systemlog.dto.SystemLogFilter;
import com.g42.platform.gms.systemlog.dto.SystemLogFilterOptionsDto;
import com.g42.platform.gms.systemlog.dto.SystemLogStatsDto;
import com.g42.platform.gms.systemlog.entity.SystemLogJpa;
import com.g42.platform.gms.systemlog.repository.SystemLogJpaRepo;
import com.g42.platform.gms.systemlog.specification.SystemLogSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemLogQueryService {

    private static final List<String> DATA_CHANGE_ACTIONS =
            List.of("CREATE", "UPDATE", "DELETE", "PERMISSION");
    private static final int EXPORT_MAX_ROWS = 10000;

    private final SystemLogJpaRepo systemLogJpaRepo;
    private final SystemLogTargetResolver targetResolver;
    private final NamedParameterJdbcTemplate jdbc;

    public Page<SystemLogDto> search(int page, int size, SystemLogFilter filter) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt", "logId"));
        Page<SystemLogDto> result = systemLogJpaRepo.findAll(spec(filter), pageable).map(this::toDto);
        targetResolver.fill(result.getContent());
        return result;
    }

    public SystemLogStatsDto stats(SystemLogFilter filter) {
        Specification<SystemLogJpa> spec = spec(filter);
        long total = systemLogJpaRepo.count(spec);
        long loginFailed = systemLogJpaRepo.count(SystemLogSpecification.withAction(spec, "LOGIN_FAILED"));
        long dataChange = systemLogJpaRepo.count(SystemLogSpecification.withActionIn(spec, DATA_CHANGE_ACTIONS));
        return new SystemLogStatsDto(total, loginFailed, dataChange);
    }

    /** Người từng thao tác (để chọn ở ô "Người làm") + các nhóm thao tác. */
    public SystemLogFilterOptionsDto filterOptions() {
        // staff_profile (utf8mb4_0900_ai_ci) khác collation với system_log (utf8mb4_unicode_ci):
        // không COALESCE hai cột trong SQL (lỗi "Illegal mix of collations"), ghép tên ở Java.
        List<SystemLogFilterOptionsDto.Actor> actors = new ArrayList<>(jdbc.query("""
                SELECT l.actor_staff_id AS staff_id,
                       MAX(sp.full_name) AS profile_name,
                       MAX(l.actor_name) AS log_name,
                       MAX(l.actor_role) AS role,
                       COUNT(*) AS cnt
                FROM system_log l
                LEFT JOIN staff_profile sp ON sp.staff_id = l.actor_staff_id
                WHERE l.actor_staff_id IS NOT NULL
                GROUP BY l.actor_staff_id""",
                new MapSqlParameterSource(),
                (rs, i) -> {
                    String name = rs.getString("profile_name");
                    if (name == null || name.isBlank()) name = rs.getString("log_name");
                    if (name == null || name.isBlank()) name = "Nhân viên #" + rs.getInt("staff_id");
                    return new SystemLogFilterOptionsDto.Actor(
                            rs.getInt("staff_id"), name, rs.getString("role"), rs.getLong("cnt"));
                }));
        java.text.Collator vi = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("vi"));
        actors.sort((a, b) -> vi.compare(a.name(), b.name()));
        List<SystemLogFilterOptionsDto.Operation> operations = Arrays.stream(SystemLogOperation.values())
                .map(op -> new SystemLogFilterOptionsDto.Operation(op.name(), op.label()))
                .toList();
        return new SystemLogFilterOptionsDto(actors, operations);
    }

    /** Xuất CSV (UTF-8 BOM để Excel hiển thị đúng tiếng Việt), tối đa EXPORT_MAX_ROWS dòng. */
    public byte[] exportCsv(SystemLogFilter filter) {
        List<SystemLogJpa> rows = systemLogJpaRepo.findAll(spec(filter),
                PageRequest.of(0, EXPORT_MAX_ROWS, Sort.by(Sort.Direction.DESC, "createdAt", "logId"))).getContent();
        List<SystemLogDto> dtos = new ArrayList<>(rows.stream().map(this::toDto).toList());
        // Tra tên đối tượng theo từng khối cho khỏi một câu IN quá dài
        for (int from = 0; from < dtos.size(); from += 100) {
            targetResolver.fill(dtos.subList(from, Math.min(from + 100, dtos.size())));
        }

        StringBuilder sb = new StringBuilder("﻿");
        sb.append("ID,Thời gian,Nhân viên,Vai trò,Hành động,Phân hệ,Mức độ,Nội dung,Đối tượng,Thay đổi,Mã đối tượng,Chức năng (API),IP,Trình duyệt\n");
        for (SystemLogDto r : dtos) {
            sb.append(r.getLogId()).append(',')
              .append(csv(r.getCreatedAt() == null ? "" : r.getCreatedAt().toString())).append(',')
              .append(csv(r.getActorName())).append(',')
              .append(csv(r.getActorRole())).append(',')
              .append(csv(r.getAction())).append(',')
              .append(csv(r.getModule())).append(',')
              .append(csv(r.getSeverity())).append(',')
              .append(csv(r.getSummary())).append(',')
              .append(csv(r.getTargetName())).append(',')
              .append(csv(r.getChanges() == null ? "" : String.join("\n", r.getChanges()))).append(',')
              .append(csv(r.getTargetId())).append(',')
              .append(csv(r.getApi())).append(',')
              .append(csv(r.getIpAddress())).append(',')
              .append(csv(r.getUserAgent())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private Specification<SystemLogJpa> spec(SystemLogFilter filter) {
        SystemLogFilter f = filter != null ? filter : new SystemLogFilter();
        Map<SystemLogTargetResolver.Kind, List<Integer>> hits = targetResolver.idsMatching(f.getSearch());
        return SystemLogSpecification.filter(f, hits);
    }

    private static String csv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n"))
                ? "\"" + escaped + "\"" : escaped;
    }

    private SystemLogDto toDto(SystemLogJpa r) {
        SystemLogDescriber.Described described = SystemLogDescriber.describe(r.getDescription());
        return SystemLogDto.builder()
                .logId(r.getLogId())
                .actorStaffId(r.getActorStaffId())
                .actorName(r.getActorName())
                .actorRole(r.getActorRole())
                .action(r.getAction())
                .module(r.getModule())
                .severity(r.getSeverity())
                .description(r.getDescription())
                .summary(described.summary())
                .changes(described.changes())
                .api(described.api())
                .noise(described.noise())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .ipAddress(r.getIpAddress())
                .userAgent(r.getUserAgent())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
