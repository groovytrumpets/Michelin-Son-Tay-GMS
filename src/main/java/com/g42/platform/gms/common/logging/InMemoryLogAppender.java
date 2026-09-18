package com.g42.platform.gms.common.logging;

import ch.qos.logback.classic.Level;
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
 * Được đăng ký bằng code trong {@link LogCaptureConfig}. Truy cập tĩnh vì Logback
 * tự khởi tạo appender ngoài Spring context.
 *
 * Có HAI buffer:
 * <ul>
 *   <li>{@code BUFFER} — mọi log, dùng để xem diễn biến chung.</li>
 *   <li>{@code PROBLEM_BUFFER} — chỉ WARN/ERROR, giữ riêng ra.</li>
 * </ul>
 * Tách buffer là bắt buộc chứ không phải cho gọn: chỉ cần ai đó bật lại DEBUG cho
 * Hibernate hay Spring Security là mỗi request đẻ ra hàng trăm dòng, đẩy hết lỗi ra
 * khỏi buffer chung chỉ sau vài giây — mở /backend-logs lên thì lỗi đã bay mất,
 * chỉ còn câu SQL. Buffer riêng cho WARN/ERROR khiến lỗi luôn còn đó để đọc.
 */
public class InMemoryLogAppender extends AppenderBase<ILoggingEvent> {

    /** Số dòng log tối đa giữ lại trong bộ nhớ. */
    private static final int MAX_ENTRIES = 2000;
    /** Số dòng WARN/ERROR giữ riêng, không bị log thường đẩy ra. */
    private static final int MAX_PROBLEM_ENTRIES = 1000;
    /** Số dòng stacktrace tối đa đính kèm mỗi log lỗi. */
    private static final int MAX_STACK_LINES = 40;

    private static final ConcurrentLinkedDeque<LogEntry> BUFFER = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<LogEntry> PROBLEM_BUFFER = new ConcurrentLinkedDeque<>();
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
        appendThrowable(message, event.getThrowableProxy(), 0);

        Level level = event.getLevel();
        LogEntry entry = new LogEntry(
                SEQUENCE.incrementAndGet(),
                event.getTimeStamp(),
                level != null ? level.toString() : "INFO",
                event.getLoggerName(),
                event.getThreadName(),
                message.toString()
        );

        BUFFER.addLast(entry);
        while (BUFFER.size() > MAX_ENTRIES) {
            BUFFER.pollFirst();
        }

        if (level != null && level.isGreaterOrEqual(Level.WARN)) {
            PROBLEM_BUFFER.addLast(entry);
            while (PROBLEM_BUFFER.size() > MAX_PROBLEM_ENTRIES) {
                PROBLEM_BUFFER.pollFirst();
            }
        }
    }

    /**
     * Ghi exception kèm toàn bộ chuỗi "Caused by" — nguyên nhân thật của lỗi 500
     * thường nằm ở tầng cuối cùng chứ không phải exception ngoài cùng.
     */
    private void appendThrowable(StringBuilder message, IThrowableProxy throwable, int depth) {
        if (throwable == null || depth > 5) return;

        message.append('\n');
        if (depth > 0) message.append("Caused by: ");
        message.append(throwable.getClassName());
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

        appendThrowable(message, throwable.getCause(), depth + 1);
    }

    /** Snapshot mọi log hiện có (theo thứ tự cũ → mới). */
    public static List<LogEntry> snapshot() {
        return new ArrayList<>(BUFFER);
    }

    /** Snapshot riêng WARN/ERROR — không bị log DEBUG/INFO đẩy ra. */
    public static List<LogEntry> problemSnapshot() {
        return new ArrayList<>(PROBLEM_BUFFER);
    }
}
