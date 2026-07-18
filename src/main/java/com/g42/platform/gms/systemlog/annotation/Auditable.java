package com.g42.platform.gms.systemlog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gắn lên method controller/service mutating để tự động ghi vào system_log
 * (AuditAspect xử lý sau khi method chạy thành công).
 *
 * Ví dụ:
 * <pre>
 * &#64;Auditable(action = "UPDATE", module = "CUSTOMER", description = "Cập nhật hồ sơ khách hàng", targetType = "CUSTOMER")
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /** LOGIN, LOGIN_FAILED, CREATE, UPDATE, DELETE, PERMISSION, EXPORT, OTHER */
    String action();

    /** AUTH, STAFF, CUSTOMER, BOOKING, SERVICE_TICKET, WAREHOUSE, PROMOTION, BILLING... */
    String module();

    /** INFO | WARNING | CRITICAL */
    String severity() default "INFO";

    /** Mô tả tiếng Việt cố định; aspect sẽ nối thêm tóm tắt tham số scalar. */
    String description() default "";

    /** Loại đối tượng bị tác động (điền vào target_type). */
    String targetType() default "";
}
