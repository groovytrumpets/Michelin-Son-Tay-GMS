package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.api.dto.RemindSearchDto;
import com.g42.platform.gms.estimation.domain.entity.ServiceReminder;
import com.g42.platform.gms.estimation.domain.exception.EstimateErrorCode;
import com.g42.platform.gms.estimation.domain.exception.EstimateException;
import com.g42.platform.gms.estimation.domain.repository.RemindRepo;
import com.g42.platform.gms.estimation.infrastructure.entity.ServiceReminderJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.ServiceReminderJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.ServiceRemindJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
public class RemindRepoImpl implements RemindRepo {
    @Autowired
    private ServiceRemindJpaRepo serviceRemindJpaRepo;
    @Autowired
    private ServiceReminderJpaMapper serviceReminderJpaMapper;


    @Override
    public ServiceReminder save(ServiceReminder domain, Integer staffId) {
        if (domain == null) {
            throw new EstimateException("Request Null", EstimateErrorCode.BAD_REQUEST);
        }
        if (domain.getServiceTicketId() == null||domain.getVehicleId() == null||domain.getCustomerId() == null) {
            throw new EstimateException("Required Info Null", EstimateErrorCode.BAD_REQUEST);
        }
        ServiceReminderJpa serviceReminderJpa = serviceReminderJpaMapper.toJpa(domain);
        serviceReminderJpa.setStaffId(staffId);
        serviceReminderJpa.setCreatedAt(Instant.now());
        serviceReminderJpa.setStatus("PENDING");
        return serviceReminderJpaMapper.toDomain(serviceRemindJpaRepo.save(serviceReminderJpa));
    }

    @Override
    public List<ServiceReminder> findByServiceTicket(Integer serviceTicketId) {
        return serviceRemindJpaRepo.findAllByServiceTicketId(serviceTicketId).stream().map(serviceReminderJpaMapper::toDomain).toList();
    }

    @Override
    public List<ServiceReminder> findByCustomerId(Integer customerId) {
        return serviceRemindJpaRepo.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(serviceReminderJpaMapper::toDomain).toList();
    }
    @Override
    public List<ServiceReminder> findByVehicleId(Integer vehicleId) {
        return serviceRemindJpaRepo.findAllByVehicleIdOrderByCreatedAtDesc(vehicleId).stream().map(serviceReminderJpaMapper::toDomain).toList();
    }

    @Override
    public List<ServiceReminder> findByCusIdAndVehicle(Integer customerId, Integer vehicleId) {
        return serviceRemindJpaRepo.findAllByCustomerIdAndVehicleIdOrderByCreatedAtDesc(customerId,vehicleId).stream().map(serviceReminderJpaMapper::toDomain).toList();
    }

    @Override
    public ServiceReminder updateStatusRemind(Integer remindId, String status, String reason) {
        ServiceReminderJpa serviceReminderJpa = serviceRemindJpaRepo.findById(remindId).orElse(null);
        if (serviceReminderJpa == null) {
            throw new EstimateException("Remind Not Found", EstimateErrorCode.REMIND_404);
        }
        serviceReminderJpa.setStatus(status);
        serviceReminderJpa.setUpdatedAt(Instant.now());
        serviceReminderJpa.setReason(reason);
        return serviceReminderJpaMapper.toDomain(serviceRemindJpaRepo.save(serviceReminderJpa));
    }

    @Override
    public Page<RemindSearchDto> searchReminders(int page, int size, LocalDateTime date, String status, String search, String sortBy) {
        Sort.Direction direction = Sort.Direction.DESC;
        if (sortBy != null && !sortBy.isEmpty()) {
            if (sortBy.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            } else if (sortBy.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            }
        }
        Sort sort = Sort.by(direction, "reminderDate");
        Pageable pageable = PageRequest.of(page, size, sort);

        // 2. Chuyển đổi Ngày (bỏ qua Giờ để lọc chính xác)
        LocalDate datePart = (date != null) ? date.toLocalDate() : null;

        // 3. Gọi thẳng Repository lấy kết quả
        return serviceRemindJpaRepo.searchAllCustom(status, datePart, search, pageable);
    }
}
