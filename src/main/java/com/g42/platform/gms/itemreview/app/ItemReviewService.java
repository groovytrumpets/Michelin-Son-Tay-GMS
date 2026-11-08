package com.g42.platform.gms.itemreview.app;

import com.g42.platform.gms.itemreview.api.ItemReviewDtos.*;
import com.g42.platform.gms.itemreview.infrastructure.ItemReviewJpa;
import com.g42.platform.gms.itemreview.infrastructure.ItemReviewJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Đánh giá sao từng mặt hàng (phụ tùng / dịch vụ / combo) trên website khách.
 *
 * Quy tắc đã chọn:
 * - Phải đăng nhập tài khoản khách mới đánh giá được; mỗi khách 1 đánh giá / mặt hàng, sửa lại được.
 * - Không bắt buộc đã mua; khách đã có phiếu HOÀN THÀNH / ĐÃ THANH TOÁN chứa mặt hàng thì gắn nhãn
 *   "Đã sử dụng tại garage" (tính lại mỗi lần gửi/sửa).
 * - Hiện ngay, không chờ duyệt; nhân viên ẩn được (kèm lý do) và trả lời công khai.
 *   Khách sửa đánh giá đang bị ẩn thì vẫn ẩn — không tự gỡ quyết định của nhân viên.
 *
 * Đọc gộp (trung bình, phân bố sao, danh sách có tên mặt hàng/khách) bằng JDBC thay vì JPQL:
 * có 4 entity cùng trỏ bảng catalog_item, viết JPQL dễ trỏ nhầm entity (xem bẫy #5 khi gỡ lỗi).
 */
@Service
@RequiredArgsConstructor
public class ItemReviewService {

    private static final int MAX_COMMENT = 2000;
    private static final int MAX_REPLY = 2000;

    private final ItemReviewJpaRepo repo;
    private final NamedParameterJdbcTemplate jdbc;

    // ================================================================== công khai

    public Summary summary(int catalogItemId) {
        Map<Integer, Integer> dist = new LinkedHashMap<>();
        for (int s = 5; s >= 1; s--) dist.put(s, 0);
        int[] verified = {0};
        jdbc.query("""
                SELECT rating, COUNT(*) AS cnt, SUM(CASE WHEN verified_purchase THEN 1 ELSE 0 END) AS ver
                FROM item_review
                WHERE catalog_item_id = :id AND status = 'VISIBLE'
                GROUP BY rating
                """, Map.of("id", catalogItemId), rs -> {
            int rating = rs.getInt("rating");
            if (rating >= 1 && rating <= 5) dist.put(rating, rs.getInt("cnt"));
            verified[0] += rs.getInt("ver");
        });
        int count = dist.values().stream().mapToInt(Integer::intValue).sum();
        double avg = count == 0 ? 0 : round1(dist.entrySet().stream()
                .mapToDouble(e -> e.getKey() * (double) e.getValue()).sum() / count);
        return new Summary(catalogItemId, avg, count, verified[0], dist);
    }

    public List<BriefSummary> briefSummaries(Collection<Integer> ids) {
        List<Integer> clean = ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().limit(200).toList();
        if (clean.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT catalog_item_id, AVG(rating) AS avg_rating, COUNT(*) AS cnt
                FROM item_review
                WHERE status = 'VISIBLE' AND catalog_item_id IN (:ids)
                GROUP BY catalog_item_id
                """, Map.of("ids", clean), (rs, i) -> new BriefSummary(
                rs.getInt("catalog_item_id"), round1(rs.getDouble("avg_rating")), rs.getInt("cnt")));
    }

    public PublicReviewsResponse publicReviews(int catalogItemId, int page, int size, Integer rating, String sort) {
        page = Math.max(0, page);
        size = Math.min(50, Math.max(1, size));
        MapSqlParameterSource p = new MapSqlParameterSource("id", catalogItemId);
        String where = "r.catalog_item_id = :id AND r.status = 'VISIBLE'";
        if (rating != null && rating >= 1 && rating <= 5) {
            where += " AND r.rating = :rating";
            p.addValue("rating", rating);
        }
        String order = switch (sort == null ? "" : sort) {
            case "highest" -> "r.rating DESC, r.created_at DESC";
            case "lowest" -> "r.rating ASC, r.created_at DESC";
            // Mặc định: có nội dung + đã mua lên trước, rồi mới nhất.
            case "newest" -> "r.created_at DESC";
            default -> "(r.comment IS NOT NULL AND r.comment <> '') DESC, r.verified_purchase DESC, r.created_at DESC";
        };
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM item_review r WHERE " + where, p, Long.class);
        p.addValue("limit", size).addValue("offset", page * size);
        List<PublicReview> rows = jdbc.query("""
                SELECT r.review_id, r.rating, r.comment, r.verified_purchase, r.created_at, r.updated_at,
                       r.staff_reply, r.replied_at, c.full_name
                FROM item_review r
                LEFT JOIN customer_profile c ON c.customer_id = r.customer_id
                WHERE %s
                ORDER BY %s
                LIMIT :limit OFFSET :offset
                """.formatted(where, order), p, (rs, i) -> new PublicReview(
                rs.getInt("review_id"), rs.getInt("rating"), rs.getString("comment"),
                maskName(rs.getString("full_name")), rs.getBoolean("verified_purchase"),
                instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("updated_at")),
                rs.getString("staff_reply"), instant(rs.getTimestamp("replied_at"))));
        long t = total == null ? 0 : total;
        return new PublicReviewsResponse(summary(catalogItemId),
                new PageResult<>(rows, page, size, t, (int) Math.ceil(t / (double) size)));
    }

    // ================================================================== khách

    public MyReviewResponse myReview(int catalogItemId, int customerId) {
        boolean verified = hasPurchased(customerId, catalogItemId);
        MyReview mine = repo.findByCatalogItemIdAndCustomerId(catalogItemId, customerId).map(this::toMine).orElse(null);
        return new MyReviewResponse(mine, verified);
    }

    @Transactional
    public MyReview upsert(int catalogItemId, int customerId, UpsertRequest req) {
        if (req == null || req.rating() == null || req.rating() < 1 || req.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn từ 1 đến 5 sao.");
        }
        String comment = req.comment() == null ? null : req.comment().trim();
        if (comment != null && comment.isEmpty()) comment = null;
        if (comment != null && comment.length() > MAX_COMMENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung đánh giá tối đa " + MAX_COMMENT + " ký tự.");
        }
        ensureItemExists(catalogItemId);

        Instant now = Instant.now();
        ItemReviewJpa r = repo.findByCatalogItemIdAndCustomerId(catalogItemId, customerId).orElseGet(() -> {
            ItemReviewJpa n = new ItemReviewJpa();
            n.setCatalogItemId(catalogItemId);
            n.setCustomerId(customerId);
            n.setStatus(ItemReviewJpa.VISIBLE);
            n.setCreatedAt(now);
            return n;
        });
        if (r.getReviewId() != null) r.setUpdatedAt(now);
        r.setRating(req.rating());
        r.setComment(comment);
        r.setVerifiedPurchase(hasPurchased(customerId, catalogItemId));
        return toMine(repo.save(r));
    }

    @Transactional
    public void deleteMine(int catalogItemId, int customerId) {
        repo.findByCatalogItemIdAndCustomerId(catalogItemId, customerId).ifPresent(repo::delete);
    }

    // ================================================================== nhân viên

    public AdminListResponse adminList(int page, int size, Integer rating, String status, Integer catalogItemId,
                                       String itemType, Boolean unreplied, String search) {
        page = Math.max(0, page);
        size = Math.min(100, Math.max(1, size));
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder where = new StringBuilder("1=1");
        if (rating != null && rating >= 1 && rating <= 5) { where.append(" AND r.rating = :rating"); p.addValue("rating", rating); }
        if (status != null && !status.isBlank()) { where.append(" AND r.status = :status"); p.addValue("status", status.trim().toUpperCase()); }
        if (catalogItemId != null) { where.append(" AND r.catalog_item_id = :itemId"); p.addValue("itemId", catalogItemId); }
        if (itemType != null && !itemType.isBlank()) { where.append(" AND ci.item_type = :itemType"); p.addValue("itemType", itemType.trim().toUpperCase()); }
        if (Boolean.TRUE.equals(unreplied)) where.append(" AND (r.staff_reply IS NULL OR r.staff_reply = '')");
        if (search != null && !search.isBlank()) {
            where.append(" AND (ci.item_name LIKE :q OR c.full_name LIKE :q OR c.phone LIKE :q OR r.comment LIKE :q)");
            p.addValue("q", "%" + search.trim() + "%");
        }
        String from = """
                FROM item_review r
                LEFT JOIN catalog_item ci ON ci.item_id = r.catalog_item_id
                LEFT JOIN customer_profile c ON c.customer_id = r.customer_id
                """;
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + from + " WHERE " + where, p, Long.class);
        p.addValue("limit", size).addValue("offset", page * size);
        List<AdminReview> rows = jdbc.query("""
                SELECT r.*, ci.item_name, ci.item_type, ci.slug, c.full_name, c.phone
                %s
                WHERE %s
                ORDER BY r.created_at DESC, r.review_id DESC
                LIMIT :limit OFFSET :offset
                """.formatted(from, where), p, (rs, i) -> new AdminReview(
                rs.getInt("review_id"), rs.getInt("catalog_item_id"), rs.getString("item_name"),
                rs.getString("item_type"), rs.getString("slug"),
                rs.getInt("customer_id"), rs.getString("full_name"), rs.getString("phone"),
                rs.getInt("rating"), rs.getString("comment"), rs.getBoolean("verified_purchase"),
                rs.getString("status"), rs.getString("hidden_reason"), rs.getString("staff_reply"),
                (Integer) rs.getObject("replied_by_staff_id"), instant(rs.getTimestamp("replied_at")),
                instant(rs.getTimestamp("created_at")), instant(rs.getTimestamp("updated_at"))));
        long t = total == null ? 0 : total;
        return new AdminListResponse(adminStats(), new PageResult<>(rows, page, size, t, (int) Math.ceil(t / (double) size)));
    }

    private AdminStats adminStats() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       COALESCE(AVG(CASE WHEN status = 'VISIBLE' THEN rating END), 0) AS avg_rating,
                       SUM(CASE WHEN status = 'HIDDEN' THEN 1 ELSE 0 END) AS hidden,
                       SUM(CASE WHEN staff_reply IS NULL OR staff_reply = '' THEN 1 ELSE 0 END) AS unreplied,
                       SUM(CASE WHEN rating <= 2 THEN 1 ELSE 0 END) AS low_rating,
                       SUM(CASE WHEN created_at >= :since THEN 1 ELSE 0 END) AS last30
                FROM item_review
                """, Map.of("since", Timestamp.from(Instant.now().minusSeconds(30L * 86400))),
                (rs, i) -> new AdminStats(rs.getLong("total"), round1(rs.getDouble("avg_rating")),
                        rs.getLong("hidden"), rs.getLong("unreplied"), rs.getLong("low_rating"), rs.getLong("last30")));
    }

    @Transactional
    public void setStatus(int reviewId, String status, String reason) {
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!ItemReviewJpa.VISIBLE.equals(s) && !ItemReviewJpa.HIDDEN.equals(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái phải là VISIBLE hoặc HIDDEN.");
        }
        ItemReviewJpa r = find(reviewId);
        r.setStatus(s);
        String why = reason == null ? null : reason.trim();
        r.setHiddenReason(ItemReviewJpa.HIDDEN.equals(s) && why != null && !why.isEmpty()
                ? why.substring(0, Math.min(500, why.length())) : null);
        repo.save(r);
    }

    @Transactional
    public void reply(int reviewId, String reply, Integer staffId) {
        ItemReviewJpa r = find(reviewId);
        String text = reply == null ? null : reply.trim();
        if (text != null && text.length() > MAX_REPLY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phản hồi tối đa " + MAX_REPLY + " ký tự.");
        }
        if (text == null || text.isEmpty()) {
            r.setStaffReply(null);
            r.setRepliedAt(null);
            r.setRepliedByStaffId(null);
        } else {
            r.setStaffReply(text);
            r.setRepliedAt(Instant.now());
            r.setRepliedByStaffId(staffId);
        }
        repo.save(r);
    }

    // ================================================================== hỗ trợ

    /** Khách đã có phiếu hoàn thành / đã thu tiền chứa mặt hàng này (dòng báo giá không bị gỡ). */
    boolean hasPurchased(int customerId, int catalogItemId) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                  SELECT 1
                  FROM service_ticket st
                  JOIN estimate e ON e.service_ticket_id = st.service_ticket_id
                  JOIN estimate_item ei ON ei.estimate_id = e.estimate_id
                  WHERE st.customer_id = :cid
                    AND ei.item_id = :item
                    AND st.ticket_status IN ('COMPLETED', 'PAID')
                    AND COALESCE(ei.is_removed, 0) = 0
                )
                """, Map.of("cid", customerId, "item", catalogItemId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    private void ensureItemExists(int catalogItemId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM catalog_item WHERE item_id = :id AND COALESCE(is_active, 1) = 1",
                Map.of("id", catalogItemId), Integer.class);
        if (n == null || n == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm / dịch vụ.");
        }
    }

    private ItemReviewJpa find(int reviewId) {
        return repo.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá."));
    }

    private MyReview toMine(ItemReviewJpa r) {
        return new MyReview(r.getReviewId(), r.getRating(), r.getComment(), r.getStatus(), r.getHiddenReason(),
                Boolean.TRUE.equals(r.getVerifiedPurchase()), r.getCreatedAt(), r.getUpdatedAt(), r.getStaffReply());
    }

    /**
     * "Nguyễn Văn An" -> "An N." — gọi theo tên như người Việt vẫn gọi, họ chỉ giữ chữ cái đầu.
     * Một chữ -> chữ đó; không có tên -> "Khách hàng".
     */
    static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Khách hàng";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[parts.length - 1] + " " + parts[0].charAt(0) + ".";
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
