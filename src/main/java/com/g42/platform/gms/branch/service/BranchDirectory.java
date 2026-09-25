package com.g42.platform.gms.branch.service;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh sách xưởng giữ trong bộ nhớ + quyết định "request này đang ở xưởng nào".
 *
 * <p>Xưởng hiện tại là của MÁY, không phải của tài khoản: FE lưu xưởng đã chọn trong
 * localStorage và gửi kèm mọi request qua header {@value #HEADER}. Máy lễ tân đặt ở xưởng
 * nào thì cứ xưởng đó, dù ai đăng nhập — nhân viên được điều phối qua lại giữa các xưởng
 * nên gắn xưởng vào tài khoản sẽ sai ngay lần đầu họ sang xưởng kia làm.
 *
 * <p>Đọc bảng bằng JdbcTemplate chứ không qua JPA: {@link BranchStampListener} gọi lớp này
 * ngay trong {@code @PrePersist}, mà chạy truy vấn Hibernate giữa lúc đang persist sẽ kích
 * auto-flush lồng nhau. Bảng chỉ vài dòng nên đọc cả bảng, giữ {@link #TTL_MS}, và
 * {@link BranchService} xoá bộ nhớ đệm mỗi lần sửa.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BranchDirectory {

    public static final String HEADER = "X-Branch-Id";
    private static final long TTL_MS = 60_000;

    /** Cho entity listener (Hibernate tạo, không tiêm được bean) lấy ra dùng. */
    private static volatile BranchDirectory instance;

    private final JdbcTemplate jdbcTemplate;

    private volatile Map<Integer, Entry> cache;
    private volatile long loadedAt;

    public record Entry(Integer branchId, String branchCode, String branchName,
                        boolean isDefault, boolean isActive, int sortOrder) {
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    static BranchDirectory instance() {
        return instance;
    }

    /** Tên xưởng cho biểu thức MapStruct (mapper là interface, không tiêm bean vào được). */
    public static String nameFor(Integer branchId) {
        BranchDirectory d = instance;
        return d == null ? null : d.nameOf(branchId);
    }

    public void invalidate() {
        cache = null;
    }

    private Map<Integer, Entry> entries() {
        Map<Integer, Entry> snapshot = cache;
        if (snapshot != null && System.currentTimeMillis() - loadedAt < TTL_MS) {
            return snapshot;
        }
        try {
            List<Entry> rows = jdbcTemplate.query(
                    "SELECT branch_id, branch_code, branch_name, is_default, is_active, sort_order FROM branch",
                    (rs, i) -> new Entry(rs.getInt("branch_id"), rs.getString("branch_code"),
                            rs.getString("branch_name"), rs.getBoolean("is_default"),
                            rs.getBoolean("is_active"), rs.getInt("sort_order")));
            Map<Integer, Entry> fresh = new LinkedHashMap<>();
            rows.stream()
                    .sorted(Comparator.comparingInt(Entry::sortOrder).thenComparing(Entry::branchId))
                    .forEach(e -> fresh.put(e.branchId(), e));
            cache = fresh;
            loadedAt = System.currentTimeMillis();
            return fresh;
        } catch (RuntimeException e) {
            // Bảng chưa có (BE mới lên trước Liquibase 044) hoặc DB chập chờn: không được
            // làm hỏng việc tạo phiếu chỉ vì không ghi được xưởng.
            log.warn("Không đọc được bảng branch: {}", e.getMessage());
            return snapshot != null ? snapshot : Map.of();
        }
    }

    public Entry find(Integer branchId) {
        return branchId == null ? null : entries().get(branchId);
    }

    public String nameOf(Integer branchId) {
        Entry e = find(branchId);
        return e == null ? null : e.branchName();
    }

    public String codeOf(Integer branchId) {
        Entry e = find(branchId);
        return e == null ? null : e.branchCode();
    }

    /** Có đúng một xưởng đang hoạt động thì FE ẩn hết chỗ chọn / nhãn xưởng. */
    public long activeCount() {
        return entries().values().stream().filter(Entry::isActive).count();
    }

    public Integer defaultBranchId() {
        Map<Integer, Entry> all = entries();
        return all.values().stream().filter(e -> e.isDefault() && e.isActive()).map(Entry::branchId).findFirst()
                .or(() -> all.values().stream().filter(Entry::isActive).map(Entry::branchId).findFirst())
                .orElse(null);
    }

    /** Xưởng hợp lệ để ghi lên phiếu mới: tồn tại và đang hoạt động. */
    public boolean isUsable(Integer branchId) {
        Entry e = find(branchId);
        return e != null && e.isActive();
    }

    /**
     * Xưởng để ghi lên bản ghi mới: {@code requested} nếu hợp lệ, không thì header của
     * request hiện tại, không thì xưởng mặc định. Null chỉ khi bảng rỗng / chưa tạo.
     */
    public Integer resolve(Integer requested) {
        if (isUsable(requested)) {
            return requested;
        }
        Integer fromHeader = headerBranchId();
        if (isUsable(fromHeader)) {
            return fromHeader;
        }
        if (requested != null || fromHeader != null) {
            log.warn("Xưởng không hợp lệ (yêu cầu={}, header={}), dùng xưởng mặc định", requested, fromHeader);
        }
        return defaultBranchId();
    }

    public Integer resolveCurrent() {
        return resolve(null);
    }

    private static Integer headerBranchId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return null; // job nền, không có request
        }
        HttpServletRequest request = servletAttrs.getRequest();
        String raw = request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
