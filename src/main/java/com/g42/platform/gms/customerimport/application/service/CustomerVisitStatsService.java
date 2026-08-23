package com.g42.platform.gms.customerimport.application.service;

import com.g42.platform.gms.customerimport.api.dto.CustomerVisitStatsDto;
import com.g42.platform.gms.customerimport.api.dto.ImportItemDto;
import com.g42.platform.gms.customerimport.api.dto.LegacyVisitDetailDto;
import com.g42.platform.gms.customerimport.api.dto.VisitAggregateDto;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitItemJpa;
import com.g42.platform.gms.customerimport.infrastructure.entity.LegacyVisitJpa;
import com.g42.platform.gms.customerimport.infrastructure.repository.LegacyVisitItemRepository;
import com.g42.platform.gms.customerimport.infrastructure.repository.LegacyVisitRepository;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Lần cuối khách đến xưởng và tổng số lần, gộp hai nguồn lịch sử.
 *
 * Tồn tại vì cùng một khách có thể vừa có phiếu dịch vụ trong phần mềm vừa có lượt
 * nhập từ sổ Excel cũ. Đọc một nguồn thôi thì con số sai theo hai hướng: khách cũ mới
 * quay lại bị coi là "lâu chưa đến", còn khách chỉ có dữ liệu legacy thì biến mất khỏi
 * mọi danh sách.
 */
@Service
public class CustomerVisitStatsService {

    public static final String SOURCE_SYSTEM = "SYSTEM";
    public static final String SOURCE_LEGACY = "LEGACY";

    @Autowired
    private LegacyVisitRepository legacyVisitRepo;

    @Autowired
    private LegacyVisitItemRepository legacyVisitItemRepo;

    @Autowired
    private VehicleRepository vehicleRepo;

    /** Một khách. Trả về bản ghi rỗng chứ không phải null khi khách chưa từng đến. */
    @Transactional(readOnly = true)
    public CustomerVisitStatsDto statsOf(Integer customerId) {
        return statsOf(List.of(customerId)).getOrDefault(customerId, emptyStats(customerId));
    }

    /**
     * Nhiều khách cùng lúc — hai truy vấn gom nhóm cho cả trang danh sách, thay vì mỗi
     * dòng một truy vấn.
     */
    @Transactional(readOnly = true)
    public Map<Integer, CustomerVisitStatsDto> statsOf(Collection<Integer> customerIds) {
        Map<Integer, CustomerVisitStatsDto> result = new LinkedHashMap<>();
        if (customerIds == null || customerIds.isEmpty()) return result;

        Set<Integer> ids = new LinkedHashSet<>(customerIds);
        ids.remove(null);
        if (ids.isEmpty()) return result;

        for (Integer id : ids) {
            result.put(id, emptyStats(id));
        }

        for (VisitAggregateDto row : legacyVisitRepo.aggregateTicketVisits(ids)) {
            CustomerVisitStatsDto stats = result.get(row.getCustomerId());
            if (stats == null) continue;
            stats.setTicketVisitCount(toInt(row.getVisitCount()));
            applyLastVisit(stats, row.getLastVisitAt(), SOURCE_SYSTEM);
        }

        for (VisitAggregateDto row : legacyVisitRepo.aggregateLegacyVisits(ids)) {
            CustomerVisitStatsDto stats = result.get(row.getCustomerId());
            if (stats == null) continue;
            stats.setLegacyVisitCount(toInt(row.getVisitCount()));
            applyLastVisit(stats, row.getLastVisitAt(), SOURCE_LEGACY);
        }

        LocalDateTime now = LocalDateTime.now();
        for (CustomerVisitStatsDto stats : result.values()) {
            stats.setVisitCount(stats.getTicketVisitCount() + stats.getLegacyVisitCount());
            if (stats.getLastVisitAt() != null) {
                stats.setDaysSinceLastVisit((int) ChronoUnit.DAYS.between(stats.getLastVisitAt(), now));
            }
        }
        return result;
    }

    /**
     * Lịch sử lượt cũ của một khách, mới nhất trước.
     *
     * Chỉ trả về phần legacy — phiếu dịch vụ trong phần mềm đã có màn hình và API riêng,
     * gộp ở đây sẽ thành hai nguồn sự thật cho cùng một thứ.
     */
    @Transactional(readOnly = true)
    public List<LegacyVisitDetailDto> legacyHistoryOf(Integer customerId) {
        List<LegacyVisitJpa> visits = legacyVisitRepo.findByCustomerIdOrderByVisitedAtDesc(customerId);
        if (visits.isEmpty()) return List.of();

        List<Integer> visitIds = visits.stream().map(LegacyVisitJpa::getLegacyVisitId).toList();
        Map<Integer, List<ImportItemDto>> itemsByVisit = new HashMap<>();
        for (LegacyVisitItemJpa item : legacyVisitItemRepo.findByLegacyVisitIdInOrderByLineNoAsc(visitIds)) {
            itemsByVisit.computeIfAbsent(item.getLegacyVisitId(), k -> new ArrayList<>())
                    .add(new ImportItemDto(item.getCategory(), item.getItemName(),
                            item.getQuantity(), item.getUnitPrice(), item.getAmount()));
        }

        Map<Integer, String> plateByVehicle = new HashMap<>();
        List<LegacyVisitDetailDto> result = new ArrayList<>();
        for (LegacyVisitJpa visit : visits) {
            LegacyVisitDetailDto dto = new LegacyVisitDetailDto();
            dto.setLegacyVisitId(visit.getLegacyVisitId());
            dto.setVisitedAt(visit.getVisitedAt());
            dto.setHasTime(visit.getHasTime());
            dto.setDeliveredAt(visit.getDeliveredAt());
            dto.setLegacyTicketCode(visit.getLegacyTicketCode());
            dto.setOdometer(visit.getOdometer());
            dto.setCustomerNote(visit.getCustomerNote());
            dto.setServicesText(visit.getServicesText());
            dto.setTotalAmount(visit.getTotalAmount());
            dto.setDiscountAmount(visit.getDiscountAmount());
            dto.setAmountMismatch(visit.getAmountMismatch());
            dto.setCalled(visit.getCalled());
            dto.setCallSuccess(visit.getCallSuccess());
            dto.setCallNote(visit.getCallNote());
            dto.setItems(itemsByVisit.getOrDefault(visit.getLegacyVisitId(), List.of()));
            if (visit.getVehicleId() != null) {
                dto.setLicensePlate(plateByVehicle.computeIfAbsent(visit.getVehicleId(),
                        id -> vehicleRepo.findById(id).map(v -> v.getLicensePlate()).orElse(null)));
            }
            result.add(dto);
        }
        return result;
    }

    /** Giữ mốc muộn hơn giữa hai nguồn, và ghi lại mốc đó đến từ đâu. */
    private void applyLastVisit(CustomerVisitStatsDto stats, LocalDateTime candidate, String source) {
        if (candidate == null) return;
        if (stats.getLastVisitAt() == null || candidate.isAfter(stats.getLastVisitAt())) {
            stats.setLastVisitAt(candidate);
            stats.setLastVisitSource(source);
        }
    }

    private CustomerVisitStatsDto emptyStats(Integer customerId) {
        CustomerVisitStatsDto stats = new CustomerVisitStatsDto();
        stats.setCustomerId(customerId);
        return stats;
    }

    private int toInt(Long value) {
        return value == null ? 0 : value.intValue();
    }
}
