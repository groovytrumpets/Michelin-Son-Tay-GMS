package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.estimation.api.dto.InactiveCustomerDto;
import com.g42.platform.gms.estimation.api.dto.RemindReason;
import com.g42.platform.gms.estimation.api.dto.RemindSearchDto;
import com.g42.platform.gms.estimation.api.dto.ReminderCreateDto;
import com.g42.platform.gms.estimation.api.dto.ReminderRespondDto;
import com.g42.platform.gms.estimation.api.mapper.ReminderDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.ServiceReminder;
import com.g42.platform.gms.estimation.domain.exception.EstimateErrorCode;
import com.g42.platform.gms.estimation.domain.exception.EstimateException;
import com.g42.platform.gms.estimation.domain.repository.RemindRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReminderService {
    @Autowired
    private RemindRepo remindRepo;
    @Autowired
    private ReminderDtoMapper reminderDtoMapper;


    public ReminderRespondDto createReminder(ReminderCreateDto request, StaffPrincipal principal) {
        ServiceReminder reminder = remindRepo.save(reminderDtoMapper.toDomain(request),principal.getStaffId());
        return reminderDtoMapper.toResDto(reminder);

    }

    public List<ReminderRespondDto> findReminderByServiceTicket(Integer serviceTicketId) {
        return remindRepo.findByServiceTicket(serviceTicketId).stream().map(reminderDtoMapper::toResDto).toList();
    }

    public List<ReminderRespondDto> findReminderByCustomerOrVehicle(Integer customerId, Integer vehicleId) {
        if (customerId == null && vehicleId == null) {
            throw new EstimateException("CustomerId or VehicleId are null!", EstimateErrorCode.BAD_REQUEST);
        }
        List<ServiceReminder> reminder = null;
        if (customerId != null && vehicleId == null) {
            return remindRepo.findByCustomerId(customerId).stream().map(reminderDtoMapper::toResDto).toList();
        }
        if (customerId == null && vehicleId != null) {
            return remindRepo.findByVehicleId(vehicleId).stream().map(reminderDtoMapper::toResDto).toList();
        }
        return remindRepo.findByCusIdAndVehicle(customerId,vehicleId).stream().map(reminderDtoMapper::toResDto).toList();
    }

    public ReminderRespondDto updateSkippedRemind(Integer remindId, RemindReason reason) {
        ServiceReminder reminder = remindRepo.updateStatusRemind(remindId,"SKIPPED",reason.getReason());
        return reminderDtoMapper.toResDto(reminder);
    }

    public ReminderRespondDto updateConfirmedRemind(Integer remindId, RemindReason reason) {
        ServiceReminder reminder = remindRepo.updateStatusRemind(remindId,"CONFIRMED",reason.getReason());
        return reminderDtoMapper.toResDto(reminder);
    }

    public ReminderRespondDto updateCancelledRemind(Integer remindId, RemindReason reason) {
        ServiceReminder reminder = remindRepo.updateStatusRemind(remindId,"CANCELLED",reason.getReason());
        return reminderDtoMapper.toResDto(reminder);
    }

    public ReminderRespondDto updateNotifiedRemind(Integer remindId, RemindReason reason) {
        ServiceReminder reminder = remindRepo.updateStatusRemind(remindId,"NOTIFIED",reason.getReason());
        return reminderDtoMapper.toResDto(reminder);
    }

    public Page<RemindSearchDto> searchReminders(int page, int size, LocalDateTime date, String status, String search, String phone, String sortBy) {
        return remindRepo.searchReminders(page,size,date,status,search,phone,sortBy);

    }

    /**
     * Khách lâu chưa quay lại xưởng.
     *
     * Gộp hai nguồn lịch sử: phiếu dịch vụ trong phần mềm và lượt nhập từ sổ Excel cũ,
     * lấy mốc muộn hơn giữa hai bên. Khách chỉ có dữ liệu legacy mà bỏ nguồn thứ hai thì
     * sẽ không bao giờ xuất hiện ở đây.
     *
     * maxDays để trống hoặc bằng 0 nghĩa là không giới hạn cận trên. Dữ liệu sổ cũ trải
     * hơn một năm rưỡi nên khoá cứng ở 60 ngày sẽ trả về danh sách rỗng.
     */
    public List<InactiveCustomerDto> getInactiveCustomers(int minDays, Integer maxDays) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.minusDays(minDays);
        LocalDateTime startDate = (maxDays == null || maxDays <= 0) ? null : now.minusDays(maxDays);

        Map<Integer, InactiveCustomerDto> latestByCustomer = new LinkedHashMap<>();
        collectLatest(latestByCustomer, remindRepo.findLatestTicketVisits(), "SYSTEM");
        collectLatest(latestByCustomer, remindRepo.findLatestLegacyVisits(), "LEGACY");

        List<InactiveCustomerDto> result = new ArrayList<>();
        for (InactiveCustomerDto candidate : latestByCustomer.values()) {
            LocalDateTime lastVisit = candidate.getLastVisitDate();
            if (lastVisit == null || lastVisit.isAfter(endDate)) continue;
            if (startDate != null && lastVisit.isBefore(startDate)) continue;
            candidate.setDaysSinceLastVisit((int) ChronoUnit.DAYS.between(lastVisit, now));
            result.add(candidate);
        }
        result.sort(Comparator.comparing(InactiveCustomerDto::getLastVisitDate).reversed());
        return result;
    }

    /** Giữ lại mốc muộn hơn khi một khách có mặt ở cả hai nguồn. */
    private void collectLatest(Map<Integer, InactiveCustomerDto> target,
                               List<InactiveCustomerDto> candidates, String source) {
        for (InactiveCustomerDto candidate : candidates) {
            if (candidate.getCustomerId() == null || candidate.getLastVisitDate() == null) continue;
            candidate.setSource(source);
            InactiveCustomerDto current = target.get(candidate.getCustomerId());
            if (current == null || candidate.getLastVisitDate().isAfter(current.getLastVisitDate())) {
                target.put(candidate.getCustomerId(), candidate);
            }
        }
    }
}
