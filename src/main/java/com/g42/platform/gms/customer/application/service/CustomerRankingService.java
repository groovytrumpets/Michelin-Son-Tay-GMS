package com.g42.platform.gms.customer.application.service;

import com.g42.platform.gms.customer.api.dto.CustomerPointsHistoryDto;
import com.g42.platform.gms.customer.api.dto.CustomerRankingDto;
import com.g42.platform.gms.customer.domain.enums.CustomerRank;
import com.g42.platform.gms.customer.domain.enums.DealerRank;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerPointsHistoryJpa;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerPointsJpa;
import com.g42.platform.gms.customer.infrastructure.entity.PointConfigJpa;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerPointsHistoryJpaRepo;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerPointsJpaRepo;
import com.g42.platform.gms.customer.infrastructure.repository.PointConfigJpaRepo;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerRankingService {

    private static final Map<CustomerRank, Integer> RANK_MIN_POINTS = Map.of(
            CustomerRank.BRONZE,   0,
            CustomerRank.SILVER,   5000,
            CustomerRank.GOLD,     15000,
            CustomerRank.PLATINUM, 30000,
            CustomerRank.DIAMOND,  50000
    );

    private static final Map<DealerRank, Integer> DEALER_RANK_MIN_POINTS = Map.of(
            DealerRank.LEVEL_1, 0,
            DealerRank.LEVEL_2, 50000,
            DealerRank.LEVEL_3, 150000,
            DealerRank.LEVEL_4, 300000,
            DealerRank.LEVEL_5, 500000
    );

    private static final Map<CustomerRank, String> RANK_LABEL = Map.of(
            CustomerRank.BRONZE,   "Đồng",
            CustomerRank.SILVER,   "Bạc",
            CustomerRank.GOLD,     "Vàng",
            CustomerRank.PLATINUM, "Bạch Kim",
            CustomerRank.DIAMOND,  "Kim Cương"
    );

    private static final Map<DealerRank, String> DEALER_RANK_LABEL = Map.of(
            DealerRank.LEVEL_1, "Đại lý Cấp 1",
            DealerRank.LEVEL_2, "Đại lý Cấp 2",
            DealerRank.LEVEL_3, "Đại lý Cấp 3",
            DealerRank.LEVEL_4, "Đại lý Cấp 4",
            DealerRank.LEVEL_5, "Đại lý Cấp 5"
    );

    private final CustomerPointsJpaRepo pointsRepo;
    private final CustomerPointsHistoryJpaRepo historyRepo;
    private final PointConfigJpaRepo pointConfigRepo;

    private PointConfigJpa getConfig() {
        return pointConfigRepo.findById(1).orElseGet(() -> {
            PointConfigJpa config = new PointConfigJpa();
            config.setId(1);
            config.setPointsPer1000Vnd(1);
            config.setBonusPointsPerService(10);
            config.setPointsPerReferral(50);
            return config;
        });
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    @Transactional
    public CustomerRankingDto getRanking(Integer customerId) {
        CustomerPointsJpa points = getOrCreate(customerId);
        PointConfigJpa config = getConfig();
        return toDto(points, config);
    }

    @Transactional
    public CustomerRankingDto addPointsForService(Integer customerId, long amountSpent, Integer bookingId) {
        CustomerPointsJpa points = getOrCreate(customerId);
        PointConfigJpa config = getConfig();

        int spendPoints = (int) (amountSpent / 1000 * config.getPointsPer1000Vnd());
        int delta = spendPoints + config.getBonusPointsPerService();

        applyPoints(points, delta, amountSpent, config);
        saveHistory(customerId, delta, "SERVICE_PAYMENT", bookingId, amountSpent);

        CustomerPointsJpa saved = pointsRepo.save(points);
        log.info("Customer {} earned {} points for booking {}", customerId, delta, bookingId);
        return toDto(saved, config);
    }

    @Transactional
    public CustomerRankingDto addPointsForReferral(Integer customerId) {
        CustomerPointsJpa points = getOrCreate(customerId);
        PointConfigJpa config = getConfig();
        int delta = config.getPointsPerReferral();
        
        applyPoints(points, delta, 0L, config);
        saveHistory(customerId, delta, "REFERRAL_BONUS", null, 0L);
        
        return toDto(pointsRepo.save(points), config);
    }

    @Transactional
    public CustomerRankingDto adjustPoints(Integer customerId, int delta, String reason) {
        CustomerPointsJpa points = getOrCreate(customerId);
        PointConfigJpa config = getConfig();
        applyPoints(points, delta, 0L, config);
        saveHistory(customerId, delta, reason, null, 0L);
        return toDto(pointsRepo.save(points), config);
    }

    public Page<CustomerPointsHistoryDto> getHistory(Integer customerId, int page, int size) {
        return historyRepo.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size))
                .map(this::toHistoryDto);
    }

    // ─── Scheduled Reset ───────────────────────────────────────────────────────

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
            p.setCurrentDealerRank(DealerRank.LEVEL_1);
            p.setLastActivityAt(LocalDateTime.now());
            p.setPointsResetYear(Year.now().getValue());
            return pointsRepo.save(p);
        });
    }

    private Map<CustomerRank, Integer> getRankMinPoints(PointConfigJpa config) {
        return Map.of(
                CustomerRank.BRONZE,   0,
                CustomerRank.SILVER,   config.getRankSilverPoints(),
                CustomerRank.GOLD,     config.getRankGoldPoints(),
                CustomerRank.PLATINUM, config.getRankPlatinumPoints(),
                CustomerRank.DIAMOND,  config.getRankDiamondPoints()
        );
    }

    private void applyPoints(CustomerPointsJpa points, int delta, long amountSpent, PointConfigJpa config) {
        int newTotal = Math.max(0, points.getTotalPoints() + delta);
        int newLifetime = points.getLifetimePoints() + Math.max(0, delta);
        points.setTotalPoints(newTotal);
        points.setLifetimePoints(newLifetime);
        points.setCurrentRank(calculateRank(newTotal, config));
        points.setCurrentDealerRank(calculateDealerRank(newTotal, config));
        points.setLastActivityAt(LocalDateTime.now());
        points.setPointsResetYear(Year.now().getValue());
    }

    private CustomerRank calculateRank(int totalPoints, PointConfigJpa config) {
        if (totalPoints >= config.getRankDiamondPoints()) return CustomerRank.DIAMOND;
        if (totalPoints >= config.getRankPlatinumPoints()) return CustomerRank.PLATINUM;
        if (totalPoints >= config.getRankGoldPoints()) return CustomerRank.GOLD;
        if (totalPoints >= config.getRankSilverPoints())  return CustomerRank.SILVER;
        return CustomerRank.BRONZE;
    }

    private DealerRank calculateDealerRank(int totalPoints, PointConfigJpa config) {
        if (totalPoints >= config.getDealerLevel5Points()) return DealerRank.LEVEL_5;
        if (totalPoints >= config.getDealerLevel4Points()) return DealerRank.LEVEL_4;
        if (totalPoints >= config.getDealerLevel3Points()) return DealerRank.LEVEL_3;
        if (totalPoints >= config.getDealerLevel2Points())  return DealerRank.LEVEL_2;
        return DealerRank.LEVEL_1;
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

    private CustomerRankingDto toDto(CustomerPointsJpa p, PointConfigJpa config) {
        CustomerRank rank = p.getCurrentRank();
        CustomerRank[] ranks = CustomerRank.values();
        int ordinal = rank.ordinal();

        Integer pointsToNext = null;
        String nextRankName = null;
        Map<CustomerRank, Integer> rankMinPoints = getRankMinPoints(config);

        if (ordinal < ranks.length - 1) {
            CustomerRank next = ranks[ordinal + 1];
            pointsToNext = rankMinPoints.get(next) - p.getTotalPoints();
            nextRankName = RANK_LABEL.get(next);
        }

        CustomerRankingDto dto = new CustomerRankingDto();
        dto.setCustomerId(p.getCustomerId());
        dto.setTotalPoints(p.getTotalPoints());
        dto.setLifetimePoints(p.getLifetimePoints());
        dto.setCurrentRank(rank);
        dto.setRankLabelVi(RANK_LABEL.get(rank));
        dto.setCurrentDealerRank(p.getCurrentDealerRank());
        dto.setDealerRankLabelVi(DEALER_RANK_LABEL.get(p.getCurrentDealerRank()));
        dto.setPointsToNextRank(pointsToNext);
        dto.setNextRank(nextRankName);
        dto.setLastActivityAt(p.getLastActivityAt());
        dto.setPointsResetYear(p.getPointsResetYear());
        return dto;
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
