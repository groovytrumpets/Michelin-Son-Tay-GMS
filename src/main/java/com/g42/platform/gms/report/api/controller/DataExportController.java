package com.g42.platform.gms.report.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.branch.service.BranchDirectory;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService.Challenge;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService.GrantInfo;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService.Purpose;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService.Status;
import com.g42.platform.gms.common.service.SensitiveActionVerificationService.VerificationFailedException;
import com.g42.platform.gms.report.api.dto.DataExportRequest;
import com.g42.platform.gms.report.application.service.DataExportService;
import com.g42.platform.gms.report.application.service.export.DataExportOptions;
import com.g42.platform.gms.report.application.service.export.DataExportOptions.DateBasis;
import com.g42.platform.gms.report.application.service.export.DataExportType;
import com.g42.platform.gms.systemlog.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Màn /report-export — xuất báo cáo tổng hợp ra một file Excel nhiều tab.
 *  - GET    /overview → danh sách loại dữ liệu, trạng thái xác minh email, lịch sử xuất gần đây
 *  - POST   /otp      → gửi mã 6 số tới email của chính tài khoản đang đăng nhập
 *  - POST   /verify   → đổi mã lấy phiên xuất 15 phút ({@code exportToken})
 *  - DELETE /verify   → kết thúc phiên xuất sớm
 *  - POST   /download → tải file (.xlsx), bắt buộc kèm {@code exportToken}
 *
 * <p>Mỗi lần tải ghi system_log mức WARNING (module REPORT_EXPORT) kèm kỳ, loại dữ liệu và số
 * dòng từng tab — file chứa danh bạ khách và lợi nhuận, cần trả lời được "ai đã mang ra ngoài".
 */
@Slf4j
@RestController
@RequestMapping("/api/reports/data-export")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.REPORT_DATA_EXPORT + "')")
public class DataExportController {

    private static final String AUDIT_MODULE = "REPORT_EXPORT";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Purpose PURPOSE = Purpose.REPORT_EXPORT;

    private final DataExportService exportService;
    private final SensitiveActionVerificationService verification;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final BranchDirectory branchDirectory;

    public record TypeInfo(String code, String label, String description, List<String> sheets, boolean rangeBound) {}

    public record HistoryEntry(String actorName, String actorRole, String description, String ipAddress,
                               boolean failed, String createdAt) {}

    public record Overview(List<TypeInfo> types, Status verification, List<HistoryEntry> history,
                           long codeTtlSeconds, long grantTtlMinutes) {}

    public record VerifyRequest(String code) {}

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Overview>> overview(@AuthenticationPrincipal StaffPrincipal principal) {
        StaffPrincipal staff = requireStaff(principal);
        List<TypeInfo> types = Arrays.stream(DataExportType.values())
                .map(t -> new TypeInfo(t.name(), t.label(), t.description(), t.sheets(), t.rangeBound()))
                .toList();
        List<HistoryEntry> history = jdbcTemplate.query("""
                        SELECT actor_name, actor_role, description, ip_address, target_id, created_at
                        FROM system_log
                        WHERE module = ? AND action = 'EXPORT'
                        ORDER BY created_at DESC, log_id DESC
                        LIMIT 20
                        """,
                (rs, i) -> {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    return new HistoryEntry(rs.getString("actor_name"), rs.getString("actor_role"),
                            rs.getString("description"), rs.getString("ip_address"),
                            "FAILED".equals(rs.getString("target_id")),
                            createdAt == null ? null : createdAt.toLocalDateTime().toString());
                },
                AUDIT_MODULE);
        return ResponseEntity.ok(ApiResponses.success(new Overview(types, verification.status(PURPOSE, staff), history,
                SensitiveActionVerificationService.CODE_TTL.toSeconds(),
                SensitiveActionVerificationService.GRANT_TTL.toMinutes())));
    }

    @PostMapping("/otp")
    public ResponseEntity<ApiResponse<Challenge>> sendCode(@AuthenticationPrincipal StaffPrincipal principal,
                                                           HttpServletRequest request) {
        StaffPrincipal staff = requireStaff(principal);
        Challenge challenge = verification.sendCode(PURPOSE, staff, clientIp(request));
        audit("INFO", "OTHER", null, "Gửi mã xác minh xuất báo cáo tổng hợp tới " + challenge.maskedEmail());
        return ResponseEntity.ok(ApiResponses.success(challenge));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<GrantInfo>> verify(@AuthenticationPrincipal StaffPrincipal principal,
                                                         @RequestBody(required = false) VerifyRequest body) {
        StaffPrincipal staff = requireStaff(principal);
        try {
            return ResponseEntity.ok(ApiResponses.success(verification.verify(PURPOSE, staff, body != null ? body.code() : null)));
        } catch (VerificationFailedException e) {
            if (e.isLocked()) {
                audit("WARNING", "OTHER", null, "Nhập sai mã xác minh xuất báo cáo quá số lần cho phép — mã đã bị huỷ");
            }
            throw e;
        }
    }

    @DeleteMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> revoke(@AuthenticationPrincipal StaffPrincipal principal) {
        verification.revoke(PURPOSE, requireStaff(principal));
        return ResponseEntity.ok(ApiResponses.successMessage("Đã kết thúc phiên xuất báo cáo"));
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal StaffPrincipal principal,
                                           @RequestBody DataExportRequest body) throws Exception {
        StaffPrincipal staff = requireStaff(principal);
        DataExportOptions options = toOptions(body);
        verification.requireGrant(PURPOSE, staff, body.exportToken());

        Status status = verification.status(PURPOSE, staff);
        String actorName = staff.getStaffAuth().getStaffProfile() != null
                ? staff.getStaffAuth().getStaffProfile().getFullName() : "";
        DataExportService.Result result;
        try {
            result = exportService.export(options, new DataExportService.Actor(actorName, status.maskedEmail()));
        } catch (Exception e) {
            log.error("Xuất báo cáo tổng hợp thất bại", e);
            audit("INFO", "EXPORT", "FAILED", "Xuất báo cáo tổng hợp thất bại (" + describe(options) + "): " + e.getMessage());
            throw e;
        }

        String rows = result.sheetRows().entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(", "));
        audit("WARNING", "EXPORT", "OK", "Xuất " + result.filename() + " — " + describe(options) + ". Số dòng: " + rows);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", result.filename());
        headers.setCacheControl("no-store");
        return ResponseEntity.ok().headers(headers).body(result.content());
    }

    // ------------------------------------------------------------------------------------------

    private DataExportOptions toOptions(DataExportRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin xuất báo cáo.");
        }
        Set<DataExportType> types = EnumSet.noneOf(DataExportType.class);
        for (String code : body.types() != null ? body.types() : List.<String>of()) {
            try {
                types.add(DataExportType.valueOf(code.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại dữ liệu không hợp lệ: " + code);
            }
        }
        if (types.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn ít nhất một loại dữ liệu để xuất.");
        }
        LocalDate from = body.from() != null ? body.from() : LocalDate.now().withDayOfMonth(1);
        LocalDate to = body.to() != null ? body.to() : LocalDate.now();
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        DateBasis basis = "PAID".equalsIgnoreCase(body.dateBasis()) ? DateBasis.PAID : DateBasis.RECEIVED;
        return new DataExportOptions(from, to, types, basis, body.branchId(),
                "ACTIVE".equalsIgnoreCase(body.customerScope()),
                body.includeLegacy() == null || body.includeLegacy());
    }

    private String describe(DataExportOptions o) {
        return "kỳ " + o.from().format(DAY) + " – " + o.to().format(DAY)
                + ", theo " + o.basis().label().toLowerCase(Locale.ROOT)
                + ", " + (o.branchId() == null ? "mọi xưởng" : "xưởng " + branchDirectory.nameOf(o.branchId()))
                + ", gồm " + o.types().stream().map(DataExportType::label).collect(Collectors.joining(" / "))
                + (o.has(DataExportType.CUSTOMERS) ? (o.activeCustomersOnly() ? " (khách có phát sinh)" : " (cả danh bạ)") : "")
                + (o.includeLegacy() ? ", kèm sổ cũ" : "");
    }

    /** @param targetId OK | FAILED cho lượt tải file (lịch sử trên màn đọc cột này), null cho việc khác. */
    private void audit(String severity, String action, String targetId, String description) {
        auditService.record(auditService.currentContext()
                .action(action)
                .module(AUDIT_MODULE)
                .severity(severity)
                .targetType("REPORT")
                .targetId(targetId)
                .description(description)
                .build());
    }

    private static StaffPrincipal requireStaff(StaffPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ tài khoản nhân viên mới xuất được báo cáo.");
        }
        return principal;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
