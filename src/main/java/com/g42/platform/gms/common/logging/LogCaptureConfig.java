package com.g42.platform.gms.common.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký InMemoryLogAppender vào ROOT logger bằng code thay vì logback-spring.xml.
 *
 * Lý do: dự án dùng spring-boot-devtools (restart classloader riêng cho code ứng dụng),
 * nếu khai báo appender trong XML thì Logback (base classloader) có thể không nạp được
 * class appender nằm ở restart classloader — appender im lặng không hoạt động.
 * Đăng ký trong @PostConstruct đảm bảo chạy đúng ở mọi lần khởi động, kể cả hot-restart.
 */
@Configuration
public class LogCaptureConfig {

    private static final String APPENDER_NAME = "IN_MEMORY_ADMIN_VIEWER";

    @PostConstruct
    public void registerInMemoryAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        // Idempotent: gỡ appender cũ nếu còn sót lại từ lần restart trước
        if (rootLogger.getAppender(APPENDER_NAME) != null) {
            rootLogger.detachAppender(APPENDER_NAME);
        }

        InMemoryLogAppender appender = new InMemoryLogAppender();
        appender.setName(APPENDER_NAME);
        appender.setContext(context);
        appender.start();
        rootLogger.addAppender(appender);

        org.slf4j.LoggerFactory.getLogger(LogCaptureConfig.class)
                .info("Đã bật thu log backend cho trang quản trị (/api/admin/backend-logs)");
    }
}
