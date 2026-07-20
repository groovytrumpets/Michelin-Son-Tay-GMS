package com.g42.platform.gms.systemlog.aspect;

import com.g42.platform.gms.systemlog.annotation.Auditable;
import com.g42.platform.gms.systemlog.service.AuditRecord;
import com.g42.platform.gms.systemlog.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Ghi audit cho các method mutating của controller, sau khi chạy thành công.
 *
 * <p>Có hai cơ chế bổ sung cho nhau:
 * <ul>
 *   <li><b>{@link Auditable}</b> — gắn tay lên method quan trọng để có mô tả tiếng Việt,
 *       module/severity được kiểm soát chính xác (ví dụ xóa khách hàng = CRITICAL).</li>
 *   <li><b>Tự động</b> — mọi endpoint POST/PUT/PATCH/DELETE <i>chưa</i> gắn {@code @Auditable}
 *       vẫn được ghi lại (đặt lịch, dùng AI, thêm bài viết, kho, khuyến mãi...),
 *       module/action được suy ra từ package + HTTP method + tên method. Nhờ đó mọi
 *       thao tác thay đổi dữ liệu đều xuất hiện trong Nhật ký hệ thống, kể cả endpoint mới.</li>
 * </ul>
 *
 * Toàn bộ ngữ cảnh (actor, ip, user-agent) được resolve trên thread request,
 * việc ghi DB chạy async qua AuditService. Mọi lỗi trong aspect bị nuốt —
 * audit không được phép phá nghiệp vụ.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final Pattern SENSITIVE_PARAM = Pattern.compile("password|passwd|pin|token|otp|secret", Pattern.CASE_INSENSITIVE);
    private static final int MAX_ARG_STRING = 100;

    /** Package con (ngay sau com.g42.platform.gms.) → mã module hiển thị trong Nhật ký. */
    private static final Map<String, String> MODULE_BY_PACKAGE = Map.ofEntries(
            Map.entry("aiassistant", "AI"),
            Map.entry("booking", "BOOKING"),
            Map.entry("booking_management", "BOOKING"),
            Map.entry("warehouse", "WAREHOUSE"),
            Map.entry("feedback", "FEEDBACK"),
            Map.entry("promotion", "PROMOTION"),
            Map.entry("billing", "BILLING"),
            Map.entry("catalog", "CATALOG"),
            Map.entry("marketing", "SERVICE"),
            Map.entry("service_ticket_management", "SERVICE_TICKET"),
            Map.entry("customer", "CUSTOMER"),
            Map.entry("staff", "STAFF"),
            Map.entry("manager", "STAFF"),
            Map.entry("estimation", "ESTIMATION"),
            Map.entry("attendancerequest", "ATTENDANCE"),
            Map.entry("dashboard", "DASHBOARD"),
            Map.entry("vehicle", "VEHICLE"),
            Map.entry("chat", "CHAT"),
            Map.entry("notification", "NOTIFICATION")
    );

    /**
     * Các package đã có cơ chế audit riêng hoặc không nên tự động ghi (tránh trùng/nhiễu):
     * auth (LOGIN/LOGIN_FAILED xử lý ở StaffAuthService/AuthFailureListener),
     * systemlog (chính trang nhật ký), common.api (log FE đẩy lên, upload ảnh tạm).
     */
    private static final String[] EXCLUDED_PACKAGE_FRAGMENTS = {
            ".auth.", ".systemlog.", ".common.api."
    };

    private final AuditService auditService;

    // ------------------------------------------------------------------
    // 1) @Auditable — mô tả do lập trình viên gắn tay
    // ------------------------------------------------------------------

    @AfterReturning(pointcut = "@annotation(auditable)", argNames = "joinPoint,auditable")
    public void afterAuditableMethod(JoinPoint joinPoint, Auditable auditable) {
        try {
            AuditRecord.AuditRecordBuilder builder = auditService.currentContext()
                    .action(auditable.action())
                    .module(auditable.module())
                    .severity(auditable.severity())
                    .targetType(auditable.targetType().isBlank() ? null : auditable.targetType());

            String argsSummary = summarizeArgs(joinPoint);
            String description = auditable.description();
            if (!argsSummary.isBlank()) {
                description = description.isBlank() ? argsSummary : description + " (" + argsSummary + ")";
            }
            builder.description(description.isBlank() ? null : description);
            builder.targetId(resolveTargetId(joinPoint));

            auditService.record(builder.build());
        } catch (Exception e) {
            log.warn("AuditAspect failed for {}: {}", joinPoint.getSignature(), e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 2) Tự động — mọi endpoint mutating chưa gắn @Auditable
    // ------------------------------------------------------------------

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.PatchMapping)"
            + " || @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void mutatingEndpoint() {
    }

    @Pointcut("@annotation(com.g42.platform.gms.systemlog.annotation.Auditable)")
    public void hasAuditable() {
    }

    @AfterReturning(
            pointcut = "mutatingEndpoint() && !hasAuditable() && within(com.g42.platform.gms..*)",
            argNames = "joinPoint")
    public void afterMutatingEndpoint(JoinPoint joinPoint) {
        try {
            String packageName = joinPoint.getSignature().getDeclaringType().getPackageName();
            for (String fragment : EXCLUDED_PACKAGE_FRAGMENTS) {
                if (packageName.contains(fragment)) return;
            }

            String[] http = currentHttp();
            String httpMethod = http[0];
            String uri = http[1];
            String methodName = joinPoint.getSignature().getName();

            String module = deriveModule(packageName);
            String action = deriveAction(httpMethod, methodName, uri);
            String severity = "DELETE".equals(action) ? "WARNING" : "INFO";

            String base = (httpMethod != null ? httpMethod + " " : "") + (uri != null ? uri : methodName);
            String argsSummary = summarizeArgs(joinPoint);
            String description = argsSummary.isBlank() ? base : base + " (" + argsSummary + ")";

            AuditRecord.AuditRecordBuilder builder = auditService.currentContext()
                    .action(action)
                    .module(module)
                    .severity(severity)
                    .description(description)
                    .targetId(resolveTargetId(joinPoint));

            auditService.record(builder.build());
        } catch (Exception e) {
            log.warn("AuditAspect (auto) failed for {}: {}", joinPoint.getSignature(), e.getMessage());
        }
    }

    /** module suy từ package con ngay sau com.g42.platform.gms. */
    private String deriveModule(String packageName) {
        String prefix = "com.g42.platform.gms.";
        String rest = packageName.startsWith(prefix) ? packageName.substring(prefix.length()) : packageName;
        int dot = rest.indexOf('.');
        String segment = dot >= 0 ? rest.substring(0, dot) : rest;
        String mapped = MODULE_BY_PACKAGE.get(segment);
        return mapped != null ? mapped : segment.toUpperCase(Locale.ROOT);
    }

    /** action suy từ HTTP verb + tên method + đường dẫn (dùng cho endpoint chưa gắn @Auditable). */
    private String deriveAction(String httpMethod, String methodName, String uri) {
        String haystack = ((methodName != null ? methodName : "") + " " + (uri != null ? uri : ""))
                .toLowerCase(Locale.ROOT);
        if (haystack.contains("export") || haystack.contains("download")) return "EXPORT";
        if ("DELETE".equalsIgnoreCase(httpMethod) || haystack.contains("delete") || haystack.contains("remove")) {
            return "DELETE";
        }
        if (haystack.contains("lock") || haystack.contains("permission") || haystack.contains("role")
                || haystack.contains("grant") || haystack.contains("revoke")) {
            return "PERMISSION";
        }
        if ("POST".equalsIgnoreCase(httpMethod)) {
            if (haystack.contains("update") || haystack.contains("modify") || haystack.contains("edit")
                    || haystack.contains("cancel") || haystack.contains("approve") || haystack.contains("reject")
                    || haystack.contains("assign") || haystack.contains("confirm") || haystack.contains("status")
                    || haystack.contains("checkin") || haystack.contains("checkout")) {
                return "UPDATE";
            }
            return "CREATE";
        }
        // PUT / PATCH
        return "UPDATE";
    }

    /** {method, uri} của request hiện tại; phần tử null nếu không có request context. */
    private String[] currentHttp() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpServletRequest request = attrs.getRequest();
                return new String[]{request.getMethod(), request.getRequestURI()};
            }
        } catch (Exception ignored) {
        }
        return new String[]{null, null};
    }

    /**
     * Tóm tắt tham số: chỉ lấy scalar (số, chuỗi ngắn, boolean, enum) theo tên;
     * bỏ qua DTO/object phức tạp và mọi tham số có tên nhạy cảm (password/pin/token/otp).
     */
    private String summarizeArgs(JoinPoint joinPoint) {
        List<String> parts = new ArrayList<>();
        Object[] args = joinPoint.getArgs();
        String[] names = (joinPoint.getSignature() instanceof CodeSignature cs) ? cs.getParameterNames() : null;
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) continue;
            String name = (names != null && i < names.length) ? names[i] : ("arg" + i);
            if (SENSITIVE_PARAM.matcher(name).find()) continue;
            if (arg instanceof Number || arg instanceof Boolean || arg instanceof Enum<?>) {
                parts.add(name + "=" + arg);
            } else if (arg instanceof String s) {
                if (SENSITIVE_PARAM.matcher(s).find()) continue;
                parts.add(name + "=" + (s.length() > MAX_ARG_STRING ? s.substring(0, MAX_ARG_STRING) + "…" : s));
            }
        }
        return String.join(", ", parts);
    }

    /** target_id: ưu tiên tham số @PathVariable, sau đó tham số số đầu tiên. */
    private String resolveTargetId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature ms) {
            Method method = ms.getMethod();
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length && i < args.length; i++) {
                if (params[i].isAnnotationPresent(PathVariable.class) && args[i] != null) {
                    return String.valueOf(args[i]);
                }
            }
        }
        for (Object arg : args) {
            if (arg instanceof Number) return String.valueOf(arg);
        }
        return null;
    }
}
