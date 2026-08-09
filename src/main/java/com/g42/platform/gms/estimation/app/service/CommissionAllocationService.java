package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.estimation.api.dto.CommissionAllocationDto;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.CommissionAllocationJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.CommissionAllocationJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Quản lý hoa hồng 4 bên của phiếu báo giá.
 *
 * Căn cứ tính (base): tổng tiền phiếu nếu dòng hoa hồng gắn toàn phiếu, hoặc
 * thành tiền của dòng báo giá nếu có estimateItemId. Client gửi lên phần trăm
 * hoặc số tiền; server luôn tính lại để hai giá trị khớp nhau.
 */
@Service
@RequiredArgsConstructor
public class CommissionAllocationService {

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final CommissionAllocationJpaRepo commissionAllocationJpaRepo;
    private final EstimateRepository estimateRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final CustomerInternalApi customerInternalApi;

    public List<CommissionAllocationDto> getByEstimateId(Integer estimateId) {
        List<CommissionAllocationJpa> rows = commissionAllocationJpaRepo.findByEstimateId(estimateId);
        return toDtos(rows);
    }

    /**
     * Ghi đè toàn bộ hoa hồng của một phiếu báo giá.
     * Danh sách rỗng nghĩa là xóa hết hoa hồng đang có.
     */
    @Transactional
    public List<CommissionAllocationDto> replaceForEstimate(Integer estimateId,
                                                           List<CommissionAllocationDto> requests,
                                                           Integer staffId) {
        Estimate estimate = estimateRepository.findEstimateById(estimateId);
        if (estimate == null) {
            throw new IllegalArgumentException("Không tìm thấy phiếu báo giá #" + estimateId);
        }

        commissionAllocationJpaRepo.deleteByEstimateId(estimateId);
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        BigDecimal estimateTotal = estimate.getTotalPrice() != null ? estimate.getTotalPrice() : BigDecimal.ZERO;
        Map<Integer, BigDecimal> itemTotals = loadItemTotals(estimateId);

        List<CommissionAllocationJpa> toSave = new ArrayList<>();
        for (CommissionAllocationDto req : requests) {
            if (req == null || req.getPartyType() == null) continue;

            BigDecimal base = req.getEstimateItemId() != null
                    ? itemTotals.getOrDefault(req.getEstimateItemId(), BigDecimal.ZERO)
                    : estimateTotal;

            BigDecimal ratePercent = req.getRatePercent();
            BigDecimal amount;
            if (ratePercent != null) {
                amount = base.multiply(ratePercent)
                        .divide(ONE_HUNDRED, MONEY_SCALE, RoundingMode.HALF_UP);
            } else {
                amount = req.getAmount() != null
                        ? req.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                // Suy ngược ra phần trăm để hiển thị; base bằng 0 thì không suy được
                if (base.compareTo(BigDecimal.ZERO) > 0) {
                    ratePercent = amount.multiply(ONE_HUNDRED)
                            .divide(base, RATE_SCALE, RoundingMode.HALF_UP);
                }
            }

            // Bỏ qua dòng không mang giá trị gì, tránh lưu rác
            if (amount.compareTo(BigDecimal.ZERO) == 0 && req.getPartnerId() == null) continue;

            CommissionAllocationJpa row = new CommissionAllocationJpa();
            row.setEstimateId(estimateId);
            row.setEstimateItemId(req.getEstimateItemId());
            row.setPartyType(req.getPartyType());
            row.setPartnerId(req.getPartnerId());
            row.setStaffId(req.getStaffId());
            row.setBaseAmount(base);
            row.setRatePercent(ratePercent);
            row.setAmount(amount);
            row.setNote(req.getNote());
            row.setCreatedBy(staffId);
            toSave.add(row);
        }

        return toDtos(commissionAllocationJpaRepo.saveAll(toSave));
    }

    /** Tổng hoa hồng của một phiếu, dùng để trừ ra lợi nhuận thực. */
    public BigDecimal getTotalCommission(Integer estimateId) {
        return commissionAllocationJpaRepo.findByEstimateId(estimateId).stream()
                .map(row -> row.getAmount() != null ? row.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<Integer, BigDecimal> loadItemTotals(Integer estimateId) {
        Map<Integer, BigDecimal> totals = new HashMap<>();
        for (EstimateItem item : estimateItemRepository.findByEstimateId(estimateId)) {
            if (item.getId() == null) continue;
            BigDecimal value = item.getFinalPrice() != null ? item.getFinalPrice() : item.getTotalPrice();
            totals.put(item.getId(), value != null ? value : BigDecimal.ZERO);
        }
        return totals;
    }

    private List<CommissionAllocationDto> toDtos(List<CommissionAllocationJpa> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        List<Integer> partnerIds = rows.stream()
                .map(CommissionAllocationJpa::getPartnerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, String> partnerNames = new HashMap<>();
        if (!partnerIds.isEmpty()) {
            List<CustomerProfile> partners = customerInternalApi.findAllByIds(partnerIds);
            if (partners != null) {
                for (CustomerProfile partner : partners) {
                    if (partner != null && partner.getCustomerId() != null) {
                        partnerNames.put(partner.getCustomerId(), partner.getFullName());
                    }
                }
            }
        }

        return rows.stream()
                .map(row -> new CommissionAllocationDto(
                        row.getId(),
                        row.getEstimateItemId(),
                        row.getPartyType(),
                        row.getPartnerId(),
                        row.getPartnerId() == null ? null : partnerNames.get(row.getPartnerId()),
                        row.getStaffId(),
                        row.getBaseAmount(),
                        row.getRatePercent(),
                        row.getAmount(),
                        row.getNote()))
                .toList();
    }
}
