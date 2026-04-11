package com.g42.platform.gms.warehouse.app.service.commission;

import com.g42.platform.gms.warehouse.api.dto.response.CommissionReportResponse;
import com.g42.platform.gms.warehouse.domain.repository.CommissionRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CommissionRecordJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockIssueJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final StockIssueRepo stockIssueRepo;
    private final CommissionRepo commissionRepo;

    @Override
    @Transactional
    public void processCommission(Integer issueId, Integer staffId) {
        StockIssueJpa issue = stockIssueRepo.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiếu xuất id=" + issueId));

        String periodMonth = LocalDate.now().format(PERIOD_FMT);

        for (StockIssueItemJpa item : issue.getItems()) {
            commissionRepo.findActiveConfigByItem(item.getItemId())
                    .ifPresent(config -> {
                        int totalQty = commissionRepo
                                .sumQuantityByStaffAndItemAndPeriod(staffId, item.getItemId(), periodMonth);
                        int newTotal = totalQty + item.getQuantity();

                        if (newTotal >= config.getCommissionQuantityThreshold()) {
                            BigDecimal commissionValue = item.getFinalPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                                    .multiply(config.getCommissionRate())
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                            CommissionRecordJpa record = new CommissionRecordJpa();
                            record.setStaffId(staffId);
                            record.setItemId(item.getItemId());
                            record.setIssueId(issueId);
                            record.setQuantity(item.getQuantity());
                            record.setFinalPrice(item.getFinalPrice());
                            record.setCommissionRate(config.getCommissionRate());
                            record.setCommissionValue(commissionValue);
                            record.setPeriodMonth(periodMonth);
                            commissionRepo.saveRecord(record);
                        }
                    });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissionReportResponse> getCommissionReport(String periodMonth, Integer staffId) {
        List<CommissionRecordJpa> records = staffId != null
                ? commissionRepo.findRecordsByStaffAndPeriod(staffId, periodMonth)
                : commissionRepo.findRecordsByPeriod(periodMonth);

        return records.stream().map(r -> {
            CommissionReportResponse resp = new CommissionReportResponse();
            resp.setRecordId(r.getRecordId());
            resp.setStaffId(r.getStaffId());
            resp.setItemId(r.getItemId());
            resp.setIssueId(r.getIssueId());
            resp.setQuantity(r.getQuantity());
            resp.setFinalPrice(r.getFinalPrice());
            resp.setCommissionRate(r.getCommissionRate());
            resp.setCommissionValue(r.getCommissionValue());
            resp.setPeriodMonth(r.getPeriodMonth());
            resp.setCreatedAt(r.getCreatedAt());
            return resp;
        }).collect(Collectors.toList());
    }
}
