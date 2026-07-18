package com.g42.platform.gms.customer.application.service;

import com.g42.platform.gms.customer.api.dto.CustomerPointsHistoryDto;
import com.g42.platform.gms.customer.api.dto.CustomerRankingDto;
import com.g42.platform.gms.customer.domain.enums.CustomerRank;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerPointsHistoryJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerPointsJpa;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerPointsHistoryJpaRepo;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerPointsJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Map;

/**
 * Quản lý điểm tích lũy và hạng khách hàng.
 *
 * Quy tắc tích điểm:
 *  - 1 điểm / 1.000 VNĐ chi tiêu (x hệ số hạng)
 *  - +10 điểm mỗi lần sử dụng dịch vụ (x bonus hạng)
 *
 * Hạng: BRONZE(0) → SILVER(500) → GOLD(2000) → PLATINUM(5000)
 *
 * Reset: Điểm năm hiện tại = 0 nếu không dùng dịch vụ trong 12 tháng.
 *        Hạng hạ về BRONZE. Chạy tự động lúc 00:00 ngày 01/01 hàng năm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRankingService {

    private static final Map<CustomerRank, Integer> RANK_MIN_POINTS = Map.of(
            CustomerRank.BRONZE,   0,
            CustomerRank.SILVER,   500,
            CustomerRank.GOLD,     2000,
            CustomerRank.PLATINUM, 5000
    );

    // Điểm / 1000 VND (nhân theo hệ số hạng)
    private static final Map<CustomerRank, Double> SPEND_MULTIPLIER = Map.of(
            CustomerRank.BRONZE,   1.0,
            CustomerRank.SILVER,   1.2,
            CustomerRank.GOLD,     1.5,
            CustomerRank.PLATINUM, 2.0
    );

    // Điểm bonus mỗi lần dùng dịch vụ
    private static final Map<CustomerRank, Integer> VISIT_BONUS = Map.of(
            CustomerRank.BRONZE,   10,
            CustomerRank.SILVER,   15,
            CustomerRank.GOLD,     20,
            CustomerRank.PLATINUM, 30
    );

    private static final Map<CustomerRank, String> RANK_LABEL = Map.of(
            CustomerRank.BRONZE,   "Đồng",
            CustomerRank.SILVER,   "Bạc",
            CustomerRank.GOLD,     "Vàng",
            CustomerRank.PLATINUM, "Bạch Kim"
    );

    private final CustomerPointsJpaRepo pointsRepo;
    private final CustomerPointsHistoryJpaRepo historyRepo;

    // ─── Public API ────────────────────────────────────────────────────────────

    /** Lấy thông tin điểm & hạng của khách (tạo mới nếu chưa có). */
    @Transactional
    public CustomerRankingDto getRanking(Integer customerId) {
        CustomerPointsJpa points = getOrCreate(customerId);
        return toDto(points);
    }

    /**
     * Cộng điểm sau khi thanh toán dịch vụ.
     *
     * @param customerId  ID khách hàng
     * @param amountSpent Số tiền đã thanh toán (VNĐ)
     * @param bookingId   ID booking liên quan (nullable)
     */
    @Transactional
    public CustomerRankingDto addPointsForService(Integer customerId, long amountSpent, Integer bookingId) {
        CustomerPointsJpa points = getOrCreate(customerId);

        CustomerRank rank = points.getCurrentRank();
        double multiplier = SPEND_MULTIPLIER.getOrDefault(rank, 1.0);
        int visitBonus  = VISIT_BONUS.getOrDefault(rank, 10);

        int spendPoints = (int) (amountSpent / 1000 * multiplier);
        int delta = spendPoints + visitBonus;

        applyPoints(points, delta, amountSpent);
        saveHistory(customerId, delta, "SERVICE_PAYMENT", bookingId, amountSpent);

        CustomerPointsJpa saved = pointsRepo.save(points);
        log.info("Customer {} earned {} points (spend={}, visit_bonus={}) for booking {}",
                customerId, delta, spendPoints, visitBonus, bookingId);
        return toDto(saved);
    }

    /** Điều chỉnh điểm thủ công bởi admin (delta có thể âm). */
    @Transactional
    public CustomerRankingDto adjustPoints(Integer customerId, int delta, String reason) {
        CustomerPointsJpa points = getOrCreate(customerId);
        applyPoints(points, delta, 0L);
        saveHistory(customerId, delta, reason, null, 0L);
        return toDto(pointsRepo.save(points));
    }

    /** Lịch sử điểm của khách (phân trang). */
    public Page<CustomerPointsHistoryDto> getHistory(Integer customerId, int page, int size) {
        return historyRepo.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size))
                .map(this::toHistoryDto);
    }

    // ─── Scheduled Reset ───────────────────────────────────────────────────────

    /**
     * Chạy lúc 00:00 ngày 01/01 hàng năm.
     * Reset điểm về 0 + hạng về BRONZE cho khách không dùng dịch vụ
     * trong 12 tháng qua.
     */
    @Scheduled(cron = "0 0 0 1 1 *")
    @Transactional
    public void resetInactiveCustomerPoints() {
        int currentYear = Year.now().getValue();
        LocalDateTime cutoff = LocalDateTime.now().minusYears(1);
        int count = pointsRepo.resetInactivePoints(currentYear, cutoff);
        log.info("[CustomerRanking] Annual reset: {} inactive customers reset to BRONZE", count);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private CustomerPointsJpa getOrCreate(Integer customerId) {
        return pointsRepo.findByCustomerId(customerId).orElseGet(() -> {
            CustomerPointsJpa p = new CustomerPointsJpa();
            p.setCustomerId(customerId);
            p.setTotalPoints(0);
            p.setLifetimePoints(0);
            p.setCurrentRank(CustomerRank.BRONZE);
            p.setLastActivityAt(LocalDateTime.now());
            p.setPointsResetYear(Year.now().getValue());
            return pointsRepo.save(p);
        });
    }

    private void applyPoints(CustomerPointsJpa points, int delta, long amountSpent) {
        int newTotal = Math.max(0, points.getTotalPoints() + delta);
        int newLifetime = points.getLifetimePoints() + Math.max(0, delta);
        points.setTotalPoints(newTotal);
        points.setLifetimePoints(newLifetime);
        points.setCurrentRank(calculateRank(newTotal));
        points.setLastActivityAt(LocalDateTime.now());
        points.setPointsResetYear(Year.now().getValue());
    }

    private CustomerRank calculateRank(int totalPoints) {
        if (totalPoints >= 5000) return CustomerRank.PLATINUM;
        if (totalPoints >= 2000) return CustomerRank.GOLD;
        if (totalPoints >= 500)  return CustomerRank.SILVER;
        return CustomerRank.BRONZE;
    }

    private void saveHistory(Integer customerId, int delta, String reason, Integer bookingId, long amountSpent) {
        CustomerPointsHistoryJpa h = new CustomerPointsHistoryJpa();
        h.setCustomerId(customerId);
        h.setPointsDelta(delta);
        h.setReason(reason);
        h.setRefBookingId(bookingId);
        h.setAmountSpent(amountSpent);
        h.setCreatedAt(LocalDateTime.now());
        historyRepo.save(h);
    }

    private CustomerRankingDto toDto(CustomerPointsJpa p) {
        CustomerRank rank = p.getCurrentRank();
        CustomerRank[] ranks = CustomerRank.values();
        int ordinal = rank.ordinal();

        Integer pointsToNext = null;
        String nextRankName = null;
        if (ordinal < ranks.length - 1) {
            CustomerRank next = ranks[ordinal + 1];
            pointsToNext = RANK_MIN_POINTS.get(next) - p.getTotalPoints();
            nextRankName = RANK_LABEL.get(next);
        }

        return new CustomerRankingDto(
                p.getCustomerId(),
                p.getTotalPoints(),
                p.getLifetimePoints(),
                rank,
                RANK_LABEL.get(rank),
                pointsToNext,
                nextRankName,
                p.getLastActivityAt(),
                p.getPointsResetYear()
        );
    }

    private CustomerPointsHistoryDto toHistoryDto(CustomerPointsHistoryJpa h) {
        return new CustomerPointsHistoryDto(
                h.getHistoryId(),
                h.getPointsDelta(),
                h.getReason(),
                h.getRefBookingId(),
                h.getAmountSpent(),
                h.getCreatedAt()
        );
    }
}
