package com.g42.platform.gms.systemlog.aspect;

import com.g42.platform.gms.systemlog.annotation.Auditable;
import com.g42.platform.gms.systemlog.service.AuditRecord;
import com.g42.platform.gms.systemlog.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Ghi audit cho mọi method có @Auditable, sau khi method trả về thành công.
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

    private final AuditService auditService;

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
