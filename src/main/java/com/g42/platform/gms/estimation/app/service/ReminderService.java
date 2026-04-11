package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
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
import java.util.List;

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

    public Page<RemindSearchDto> searchReminders(int page, int size, LocalDateTime date, String status, String search, String sortBy) {
        return remindRepo.searchReminders(page,size,date,status,search,sortBy);

    }
}
