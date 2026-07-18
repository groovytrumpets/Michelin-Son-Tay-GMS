package com.g42.platform.gms.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logback appender giữ N log gần nhất trong bộ nhớ (ring buffer) để trang quản trị
 * xem log backend trực tiếp qua REST mà không cần SSH vào server đọc file.
 *
 * Được đăng ký trong logback-spring.xml. Truy cập tĩnh vì Logback tự khởi tạo appender
 * ngoài Spring context.
 */
public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    /** Số dòng log tối đa giữ lại trong bộ nhớ. */
    private static final int MAX_ENTRIES = 2000;
    /** Số dòng stacktrace tối đa đính kèm mỗi log lỗi. */
    private static final int MAX_STACK_LINES = 15;

    private static final ConcurrentLinkedDeque<LogEntry> BUFFER = new ConcurrentLinkedDeque<>();
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    public record LogEntry(
            long id,
            long timestamp,
            String level,
            String logger,
            String thread,
            String message
    ) {}

    @Override
    protected void append(ILoggingEvent event) {
        StringBuilder message = new StringBuilder(event.getFormattedMessage());
        IThrowableProxy throwable = event.getThrowableProxy();
        if (throwable != null) {
            message.append('\n').append(throwable.getClassName());
            if (throwable.getMessage() != null) {
                message.append(": ").append(throwable.getMessage());
            }
            StackTraceElementProxy[] stack = throwable.getStackTraceElementProxyArray();
            if (stack != null) {
                int limit = Math.min(stack.length, MAX_STACK_LINES);
                for (int i = 0; i < limit; i++) {
                    message.append("\n    ").append(stack[i].getSTEAsString());
                }
                if (stack.length > limit) {
                    message.append("\n    ... ").append(stack.length - limit).append(" dòng nữa");
                }
            }
        }

        BUFFER.addLast(new LogEntry(
                SEQUENCE.incrementAndGet(),
                event.getTimeStamp(),
                event.getLevel() != null ? event.getLevel().toString() : "INFO",
                event.getLoggerName(),
                event.getThreadName(),
                message.toString()
        ));

        while (BUFFER.size() > MAX_ENTRIES) {
            BUFFER.pollFirst();
        }
    }

    /** Snapshot log hiện có (theo thứ tự cũ → mới). */
    public static List<LogEntry> snapshot() {
        return new ArrayList<>(BUFFER);
    }
}
