package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.api.mapper.assignment.TicketAssignmentDtoMapper;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.domain.enums.AssignmentStatus;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentErrorCode;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentException;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketAssignmentRepo;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Application service cho ticket assignment.
 * Business logic nằm ở đây, không trong repo impl.
 */
@Service
@RequiredArgsConstructor
public class TicketAssignmentService {

    private final TicketAssignmentRepo ticketAssignmentRepo;
    private final TicketAssignmentDtoMapper dtoMapper;
    private final StaffProileJpaRepo staffProfileRepo;

    @Transactional(readOnly = true)
    public List<AvailableStaffDto> getAvailableStaff(Integer ticketId, String role) {
        return staffProfileRepo.findAvailableStaffByRole(role, ticketId).stream()
            .map(this::toAvailableStaffDto)
            .toList();
    }

    @Transactional
    public AssignStaffDto assignStaff(Integer ticketId, AssignStaffDto dto) {
        // Validate: mỗi ticket chỉ có 1 advisor
        boolean isAdvisorRole = "ADVISOR".equals(dto.getRoleInTicket());
        if (isAdvisorRole) {
            if (ticketAssignmentRepo.existsByTicketIdAndRole(ticketId, "ADVISOR")) {
                throw new AssignmentException("Ticket đã có advisor!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
            }
        }

        // Validate: mỗi ticket chỉ có 1 primary technician
        boolean wantPrimary = dto.getIsPrimary() != null && dto.getIsPrimary();
        if (wantPrimary) {
            if (ticketAssignmentRepo.existsPrimaryByTicketId(ticketId)) {
                throw new AssignmentException("Ticket đã có technician chính!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
            }
        }

        // Validate: staff phải rảnh (chỉ áp dụng cho TECHNICIAN, advisor không giới hạn)
        // Validate: staff phải có đúng role tương ứng với roleInTicket
        boolean staffHasRole = staffProfileRepo.existsByStaffIdAndRole(dto.getStaffId(), dto.getRoleInTicket());
        if (!staffHasRole) {
            throw new AssignmentException("Staff không có role " + dto.getRoleInTicket(), AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        if (!isAdvisorRole && !ticketAssignmentRepo.isStaffAvailable(dto.getStaffId())) {
            throw new AssignmentException("Staff đang bận!", AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        ServiceTicketAssignment assignment = new ServiceTicketAssignment();
        assignment.setServiceTicketId(ticketId);
        assignment.setStaffId(dto.getStaffId());
        assignment.setRoleInTicket(dto.getRoleInTicket());

        boolean isPrimary = dto.getIsPrimary() != null && dto.getIsPrimary();
        assignment.setIsPrimary(isPrimary);
        assignment.setNote(dto.getNote());
        assignment.setAssignedAt(Instant.now());
        assignment.setStatus(AssignmentStatus.PENDING); // Bắt đầu với PENDING, chưa làm việc

        ServiceTicketAssignment saved = ticketAssignmentRepo.save(assignment);
        
        // Nếu assign technician thành công, chuyển advisor từ PENDING sang ACTIVE
        // Nhưng technician vẫn ở PENDING cho đến khi bắt đầu làm việc thực sự
        if ("TECHNICIAN".equals(dto.getRoleInTicket())) {
            activateAdvisorAssignment(ticketId);
        }
        
        return dtoMapper.toDto(saved);
    }

    @Transactional
    public AssignStaffDto updateAssignment(Integer ticketId, Integer assignmentId, AssignStaffDto dto) {
        ServiceTicketAssignment existing = ticketAssignmentRepo.findById(assignmentId)
            .orElseThrow(() -> new AssignmentException("Assignment không tồn tại!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID));

        if (dto.getStaffId() != null) existing.setStaffId(dto.getStaffId());
        if (dto.getRoleInTicket() != null) existing.setRoleInTicket(dto.getRoleInTicket());
        if (dto.getIsPrimary() != null) existing.setIsPrimary(dto.getIsPrimary());
        if (dto.getNote() != null) existing.setNote(dto.getNote());

        ServiceTicketAssignment saved = ticketAssignmentRepo.save(existing);
        return dtoMapper.toDto(saved);
    }

    private AvailableStaffDto toAvailableStaffDto(StaffProfileJpa staff) {
        AvailableStaffDto dto = new AvailableStaffDto();
        dto.setStaffId(staff.getStaffId());
        dto.setFullName(staff.getFullName());
        dto.setPhone(staff.getPhone());
        dto.setAvatar(staff.getAvatar());
        return dto;
    }

    /**
     * Chuyển advisor từ PENDING sang ACTIVE khi bắt đầu làm việc (assign technician thành công).
     */
    @Transactional
    public void activateAdvisorAssignment(Integer ticketId) {
        List<ServiceTicketAssignment> advisorAssignments = ticketAssignmentRepo.findByTicketIdAndRole(ticketId, "ADVISOR");
        for (ServiceTicketAssignment assignment : advisorAssignments) {
            if (assignment.getStatus() == AssignmentStatus.PENDING) {
                assignment.setStatus(AssignmentStatus.ACTIVE);
                ticketAssignmentRepo.save(assignment);
            }
        }
    }
    
    /**
     * Đánh dấu assignment hoàn thành khi ticket được thanh toán.
     */
    @Transactional
    public void markAssignmentDone(Integer ticketId) {
        List<ServiceTicketAssignment> assignments = ticketAssignmentRepo.findByTicketId(ticketId);
        for (ServiceTicketAssignment assignment : assignments) {
            if (assignment.getStatus() == AssignmentStatus.ACTIVE || assignment.getStatus() == AssignmentStatus.PENDING) {
                assignment.setStatus(AssignmentStatus.DONE);
                ticketAssignmentRepo.save(assignment);
            }
        }
    }
    
    /**
     * Chuyển assignment từ PENDING sang ACTIVE khi bắt đầu làm việc thực sự.
     * Được gọi khi:
     * - Technician bấm "Bắt đầu kiểm tra an toàn"
     * - Advisor bấm "Bắt đầu làm việc"
     */
    @Transactional
    public void startWork(Integer ticketId, Integer staffId) {
        List<ServiceTicketAssignment> assignments = ticketAssignmentRepo.findByTicketId(ticketId);
        for (ServiceTicketAssignment assignment : assignments) {
            if (assignment.getStaffId().equals(staffId) && assignment.getStatus() == AssignmentStatus.PENDING) {
                assignment.setStatus(AssignmentStatus.ACTIVE);
                ticketAssignmentRepo.save(assignment);
            }
        }
    }

    /**
     * Thay đổi advisor cho ticket (chỉ dành cho lễ tân).
     * Chỉ được phép thay đổi khi advisor hiện tại đang ở trạng thái PENDING (chưa bắt đầu làm việc).
     */
    @Transactional
    public AssignStaffDto changeAdvisor(Integer ticketId, Integer newAdvisorId, String note) {
        // 1. Tìm assignment advisor hiện tại
        List<ServiceTicketAssignment> currentAssignments = ticketAssignmentRepo.findByTicketIdAndRole(ticketId, "ADVISOR");
        if (currentAssignments.isEmpty()) {
            throw new AssignmentException("Ticket chưa có advisor!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
        }
        
        ServiceTicketAssignment currentAdvisor = currentAssignments.get(0);
        
        // 2. Kiểm tra điều kiện thay đổi: chỉ được thay khi advisor đang PENDING (chưa bắt đầu làm việc)
        if (currentAdvisor.getStatus() != AssignmentStatus.PENDING) {
            throw new AssignmentException("Không thể thay đổi advisor khi đã bắt đầu làm việc (status: " + currentAdvisor.getStatus() + ")", 
                AssignmentErrorCode.UNAVAILABLE_STAFF);
        }
        
        // 3. Validate advisor mới phải rảnh và có role ADVISOR
        boolean newAdvisorHasRole = staffProfileRepo.existsByStaffIdAndRole(newAdvisorId, "ADVISOR");
        if (!newAdvisorHasRole) {
            throw new AssignmentException("Staff không có role ADVISOR", AssignmentErrorCode.UNAVAILABLE_STAFF);
        }
        
        // 4. Hủy assignment cũ
        currentAdvisor.setStatus(AssignmentStatus.CANCELLED);
        ticketAssignmentRepo.save(currentAdvisor);
        
        // 5. Tạo assignment mới với trạng thái PENDING
        ServiceTicketAssignment newAssignment = new ServiceTicketAssignment();
        newAssignment.setServiceTicketId(ticketId);
        newAssignment.setStaffId(newAdvisorId);
        newAssignment.setRoleInTicket("ADVISOR");
        newAssignment.setIsPrimary(true);
        newAssignment.setNote(note != null ? note : "Thay đổi advisor bởi lễ tân");
        newAssignment.setAssignedAt(Instant.now());
        newAssignment.setStatus(AssignmentStatus.PENDING); // Bắt đầu với PENDING
        
        ServiceTicketAssignment saved = ticketAssignmentRepo.save(newAssignment);
        return dtoMapper.toDto(saved);
    }
    
    /**
     * Hủy assignment technician (chỉ dành cho advisor).
     * Chỉ được phép hủy khi technician đang ở trạng thái PENDING.
     */
    @Transactional
    public void removeTechnician(Integer ticketId, Integer technicianId) {
        // 1. Tìm assignment technician
        List<ServiceTicketAssignment> assignments = ticketAssignmentRepo.findByTicketId(ticketId);
        ServiceTicketAssignment technicianAssignment = assignments.stream()
            .filter(a -> a.getStaffId().equals(technicianId) && "TECHNICIAN".equals(a.getRoleInTicket()))
            .findFirst()
            .orElseThrow(() -> new AssignmentException("Không tìm thấy assignment technician!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID));
        
        // 2. Kiểm tra điều kiện hủy: chỉ được hủy khi đang PENDING
        if (technicianAssignment.getStatus() != AssignmentStatus.PENDING) {
            throw new AssignmentException("Không thể hủy technician khi đã bắt đầu làm việc (status: " + technicianAssignment.getStatus() + ")", 
                AssignmentErrorCode.UNAVAILABLE_STAFF);
        }
        
        // 3. Hủy assignment
        technicianAssignment.setStatus(AssignmentStatus.CANCELLED);
        ticketAssignmentRepo.save(technicianAssignment);
    }
    
    /**
     * Thay đổi technician (chỉ dành cho advisor).
     * Hủy technician cũ và assign technician mới với trạng thái PENDING.
     */
    @Transactional
    public AssignStaffDto changeTechnician(Integer ticketId, Integer oldTechnicianId, Integer newTechnicianId, String note) {
        // 1. Hủy technician cũ (chỉ được hủy nếu đang PENDING)
        removeTechnician(ticketId, oldTechnicianId);
        
        // 2. Assign technician mới
        AssignStaffDto newTechnicianDto = new AssignStaffDto();
        newTechnicianDto.setStaffId(newTechnicianId);
        newTechnicianDto.setRoleInTicket("TECHNICIAN");
        newTechnicianDto.setIsPrimary(false); // Mặc định không phải primary
        newTechnicianDto.setNote(note != null ? note : "Thay đổi technician bởi advisor");
        
        return assignStaff(ticketId, newTechnicianDto);
    }
}