package com.g42.platform.gms.service_ticket_management.application.service;


import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.StaffWorkloadDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.WorkloadTicketDto;
import com.g42.platform.gms.service_ticket_management.api.mapper.assignment.TicketAssignmentDtoMapper;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.domain.enums.AssignmentStatus;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentErrorCode;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentException;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketAssignmentRepo;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


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
    private final ServiceTicketRepo serviceTicketRepo;


    @Transactional(readOnly = true)
    public List<AvailableStaffDto> getAvailableStaff(Integer ticketId, String role) {
        // Lấy tất cả staff có role, sắp xếp theo workload tăng dần (ít việc lên trước)
        List<StaffProfileJpa> staffList = staffProfileRepo.findAllByRole(role);

        return staffList.stream()
                .map(staff -> toAvailableStaffDto(staff, ticketId))
                .sorted(Comparator.comparingInt(dto -> (dto.getTotalWorkload() == null ? 0 : dto.getTotalWorkload())))
                .toList();
    }


    @Transactional(readOnly = true)
    public List<AssignStaffDto> getAssignments(Integer ticketId) {
        List<AssignStaffDto> assignments = ticketAssignmentRepo.findByTicketId(ticketId).stream()
                .map(dtoMapper::toDto)
                .toList();
        return enrichDtosWithStaffName(assignments);
    }


    @Transactional
    public AssignStaffDto assignStaff(Integer ticketId, AssignStaffDto dto) {
        // Guard: không cho assign khi phiếu đã kết thúc
        requireTicketEditable(ticketId);

        // Validate: mỗi ticket chỉ có 1 advisor
        boolean isAdvisorRole = "ADVISOR".equals(dto.getRoleInTicket());
        if (isAdvisorRole) {
            if (ticketAssignmentRepo.existsByTicketIdAndRole(ticketId, "ADVISOR")) {
                throw new AssignmentException("Ticket đã có advisor!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
            }
        }

        // Validate: staff phải có đúng role tương ứng với roleInTicket
        boolean staffHasRole = staffProfileRepo.existsByStaffIdAndRole(dto.getStaffId(), dto.getRoleInTicket());
        if (!staffHasRole) {
            throw new AssignmentException("Staff không có role " + dto.getRoleInTicket(), AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        // Validate: technician không được assign vào cùng 1 ticket 2 lần
        if ("TECHNICIAN".equals(dto.getRoleInTicket())) {
            if (ticketAssignmentRepo.isStaffAssignedToTicket(dto.getStaffId(), ticketId)) {
                throw new AssignmentException(
                    "Kỹ thuật viên đã được phân công vào phiếu này rồi!",
                    AssignmentErrorCode.UNAVAILABLE_STAFF
                );
            }
        }

        ServiceTicketAssignment assignment = new ServiceTicketAssignment();
        assignment.setServiceTicketId(ticketId);
        assignment.setStaffId(dto.getStaffId());
        assignment.setRoleInTicket(dto.getRoleInTicket());

        // Chỉ advisor mới là primary, technician luôn false
        boolean isPrimary = isAdvisorRole;
        assignment.setIsPrimary(isPrimary);
        assignment.setNote(dto.getNote());
        assignment.setAssignedAt(Instant.now());
        assignment.setStatus(AssignmentStatus.PENDING);

        ServiceTicketAssignment saved = ticketAssignmentRepo.save(assignment);

        // Nếu assign technician thành công, chuyển advisor từ PENDING sang ACTIVE
        if ("TECHNICIAN".equals(dto.getRoleInTicket())) {
            activateAdvisorAssignment(ticketId);
        }

        return enrichDtoWithStaffName(dtoMapper.toDto(saved));
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
        return enrichDtoWithStaffName(dtoMapper.toDto(saved));
    }


    @Transactional
    public AssignStaffDto cancelAssignmentById(Integer ticketId, Integer assignmentId) {
        ServiceTicketAssignment existing = ticketAssignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new AssignmentException("Assignment không tồn tại!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID));


        if (!ticketId.equals(existing.getServiceTicketId())) {
            throw new AssignmentException("Assignment không thuộc ticket này!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
        }


        if (!"TECHNICIAN".equals(existing.getRoleInTicket())) {
            throw new AssignmentException("Chỉ hủy assignment TECHNICIAN ở API này!", AssignmentErrorCode.UNAVAILABLE_STAFF);
        }


        if (existing.getStatus() != AssignmentStatus.PENDING) {
            throw new AssignmentException(
                    "Không thể hủy technician khi đã bắt đầu làm việc (status: " + existing.getStatus() + ")",
                    AssignmentErrorCode.UNAVAILABLE_STAFF
            );
        }


        existing.setStatus(AssignmentStatus.CANCELLED);
        ServiceTicketAssignment saved = ticketAssignmentRepo.save(existing);
        return enrichDtoWithStaffName(dtoMapper.toDto(saved));
    }


    private AvailableStaffDto toAvailableStaffDto(StaffProfileJpa staff, Integer ticketId) {
        AvailableStaffDto dto = new AvailableStaffDto();
        dto.setStaffId(staff.getStaffId());
        dto.setFullName(staff.getFullName());
        dto.setPhone(staff.getPhone());
        dto.setAvatar(staff.getAvatar());

        long activeCount = ticketAssignmentRepo.findByStaffId(staff.getStaffId()).stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE || a.getStatus() == AssignmentStatus.PENDING)
                .count();
        dto.setTotalWorkload((int) activeCount);
        dto.setIsBusy(activeCount > 0);
        if (activeCount > 0) {
            dto.setBusyNote("Đang làm " + activeCount + " dịch vụ");
        }
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
        requireTicketEditable(ticketId);
        // 1. Tìm assignment advisor hiện tại
        List<ServiceTicketAssignment> currentAssignments = ticketAssignmentRepo.findByTicketIdAndRole(ticketId, "ADVISOR");
        if (currentAssignments.isEmpty()) {
            throw new AssignmentException("Ticket chưa có advisor!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
        }

        ServiceTicketAssignment currentAdvisor = currentAssignments.get(0);

        // 2. Kiểm tra điều kiện thay đổi: lễ tân chỉ được đổi khi advisor PENDING
        if (currentAdvisor.getStatus() != AssignmentStatus.PENDING) {
            throw new AssignmentException(
                "Lễ tân chỉ được đổi advisor khi advisor chưa bắt đầu làm việc (PENDING). Hiện tại: " + currentAdvisor.getStatus(),
                AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        // 3. Validate advisor mới phải có role ADVISOR
        boolean newAdvisorHasRole = staffProfileRepo.existsByStaffIdAndRole(newAdvisorId, "ADVISOR");
        if (!newAdvisorHasRole) {
            throw new AssignmentException("Staff không có role ADVISOR", AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        // 4. Hủy assignment cũ
        currentAdvisor.setStatus(AssignmentStatus.CANCELLED);
        ticketAssignmentRepo.save(currentAdvisor);

        // 5. Tạo assignment mới với PENDING (lễ tân đổi thì advisor mới bắt đầu từ PENDING)
        AssignmentStatus newStatus = AssignmentStatus.PENDING;

        ServiceTicketAssignment newAssignment = new ServiceTicketAssignment();
        newAssignment.setServiceTicketId(ticketId);
        newAssignment.setStaffId(newAdvisorId);
        newAssignment.setRoleInTicket("ADVISOR");
        newAssignment.setIsPrimary(true);
        newAssignment.setNote(note != null ? note : "Thay đổi advisor bởi lễ tân");
        newAssignment.setAssignedAt(Instant.now());
        newAssignment.setStatus(newStatus);

        ServiceTicketAssignment saved = ticketAssignmentRepo.save(newAssignment);
        return enrichDtoWithStaffName(dtoMapper.toDto(saved));
    }

    /**
     * Advisor tự đổi sang advisor khác — khi đang PENDING hoặc ACTIVE.
     * Advisor mới kế thừa status của advisor cũ (PENDING → PENDING, ACTIVE → ACTIVE).
     */
    @Transactional
    public AssignStaffDto changeAdvisorByAdvisor(Integer ticketId, Integer newAdvisorId, String note) {
        requireTicketEditable(ticketId);
        List<ServiceTicketAssignment> currentAssignments = ticketAssignmentRepo.findByTicketIdAndRole(ticketId, "ADVISOR");
        if (currentAssignments.isEmpty()) {
            throw new AssignmentException("Ticket chưa có advisor!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
        }

        ServiceTicketAssignment currentAdvisor = currentAssignments.get(0);

        // Advisor được đổi khi PENDING hoặc ACTIVE
        if (currentAdvisor.getStatus() != AssignmentStatus.PENDING
                && currentAdvisor.getStatus() != AssignmentStatus.ACTIVE) {
            throw new AssignmentException(
                "Advisor chỉ có thể đổi người khi đang PENDING hoặc ACTIVE. Hiện tại: " + currentAdvisor.getStatus(),
                AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        boolean newAdvisorHasRole = staffProfileRepo.existsByStaffIdAndRole(newAdvisorId, "ADVISOR");
        if (!newAdvisorHasRole) {
            throw new AssignmentException("Staff không có role ADVISOR", AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        // Lưu status trước khi cancel để advisor mới kế thừa
        AssignmentStatus inheritedStatus = currentAdvisor.getStatus();
        currentAdvisor.setStatus(AssignmentStatus.CANCELLED);
        ticketAssignmentRepo.save(currentAdvisor);

        ServiceTicketAssignment newAssignment = new ServiceTicketAssignment();
        newAssignment.setServiceTicketId(ticketId);
        newAssignment.setStaffId(newAdvisorId);
        newAssignment.setRoleInTicket("ADVISOR");
        newAssignment.setIsPrimary(true);
        newAssignment.setNote(note != null ? note : "Thay đổi advisor bởi advisor");
        newAssignment.setAssignedAt(Instant.now());
        newAssignment.setStatus(inheritedStatus); // PENDING → PENDING, ACTIVE → ACTIVE

        ServiceTicketAssignment saved = ticketAssignmentRepo.save(newAssignment);
        return enrichDtoWithStaffName(dtoMapper.toDto(saved));
    }

    /**
     * Hủy assignment technician (chỉ dành cho advisor).
     * Cho phép hủy khi PENDING hoặc ACTIVE.
     * Trả về status của technician bị hủy để caller có thể kế thừa.
     */
    @Transactional
    public AssignmentStatus removeTechnician(Integer ticketId, Integer technicianId) {
        requireTicketEditable(ticketId);
        List<ServiceTicketAssignment> assignments = ticketAssignmentRepo.findByTicketId(ticketId);
        List<ServiceTicketAssignment> technicianAssignments = assignments.stream()
                .filter(a -> a.getStaffId().equals(technicianId) && "TECHNICIAN".equals(a.getRoleInTicket()))
                .sorted(Comparator.comparing(
                        ServiceTicketAssignment::getAssignedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();

        if (technicianAssignments.isEmpty()) {
            throw new AssignmentException("Khong tim thay assignment technician!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
        }

        // Ưu tiên PENDING, nếu không có thì lấy ACTIVE
        Optional<ServiceTicketAssignment> pendingAssignment = technicianAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.PENDING)
                .findFirst();
        Optional<ServiceTicketAssignment> activeAssignment = technicianAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE)
                .findFirst();

        ServiceTicketAssignment technicianAssignment = pendingAssignment
                .orElse(activeAssignment.orElse(null));

        if (technicianAssignment == null) {
            throw new AssignmentException(
                "Không thể hủy technician khi không ở trạng thái PENDING hoặc ACTIVE",
                AssignmentErrorCode.UNAVAILABLE_STAFF
            );
        }

        if (technicianAssignment.getStatus() != AssignmentStatus.PENDING
                && technicianAssignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new AssignmentException(
                "Không thể hủy technician khi assignment không ở trạng thái PENDING hoặc ACTIVE (status: " + technicianAssignment.getStatus() + ")",
                AssignmentErrorCode.UNAVAILABLE_STAFF
            );
        }

        AssignmentStatus previousStatus = technicianAssignment.getStatus();
        technicianAssignment.setStatus(AssignmentStatus.CANCELLED);
        ticketAssignmentRepo.save(technicianAssignment);
        return previousStatus; // trả về để changeTechnician kế thừa
    }


    /**
     * Thay đổi technician (chỉ dành cho advisor).
     * Hủy technician cũ và assign technician mới — kế thừa status (PENDING → PENDING, ACTIVE → ACTIVE).
     */
    @Transactional
    public AssignStaffDto changeTechnician(Integer ticketId, Integer oldTechnicianId, Integer newTechnicianId, String note) {
        // 1. Hủy technician cũ, lấy status để kế thừa
        AssignmentStatus inheritedStatus = removeTechnician(ticketId, oldTechnicianId);

        // 2. Assign technician mới với status kế thừa
        ServiceTicketAssignment newAssignment = new ServiceTicketAssignment();
        newAssignment.setServiceTicketId(ticketId);
        newAssignment.setStaffId(newTechnicianId);
        newAssignment.setRoleInTicket("TECHNICIAN");
        newAssignment.setIsPrimary(false);
        newAssignment.setNote(note != null ? note : "Thay đổi technician bởi advisor");
        newAssignment.setAssignedAt(Instant.now());
        newAssignment.setStatus(inheritedStatus); // PENDING → PENDING, ACTIVE → ACTIVE

        ServiceTicketAssignment saved = ticketAssignmentRepo.save(newAssignment);
        return enrichDtoWithStaffName(dtoMapper.toDto(saved));
    }
    private AssignStaffDto enrichDtoWithStaffName(AssignStaffDto dto) {
        if (dto == null || dto.getStaffId() == null) return dto;


        StaffProfileJpa staff = staffProfileRepo.findByStaffId(dto.getStaffId());
        if (staff != null && staff.getFullName() != null && !staff.getFullName().isBlank()) {
            dto.setFullName(staff.getFullName());
        }
        return dto;
    }


    private List<AssignStaffDto> enrichDtosWithStaffName(List<AssignStaffDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return dtos;


        Set<Integer> staffIds = dtos.stream()
                .map(AssignStaffDto::getStaffId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (staffIds.isEmpty()) return dtos;


        Map<Integer, String> fullNameByStaffId = staffProfileRepo.findAllById(staffIds).stream()
                .filter(s -> s.getStaffId() != null)
                .collect(Collectors.toMap(
                        StaffProfileJpa::getStaffId,
                        s -> s.getFullName() == null ? "" : s.getFullName(),
                        (a, b) -> a
                ));


        for (AssignStaffDto dto : dtos) {
            if (dto == null || dto.getStaffId() == null) continue;
            String fullName = fullNameByStaffId.get(dto.getStaffId());
            if (fullName != null && !fullName.isBlank()) {
                dto.setFullName(fullName);
            }
        }
        return dtos;
    }

    // ===================== WORKLOAD METHODS =====================

    /**
     * Lấy workload của tất cả staff, có thể filter theo role.
     */
    @Transactional(readOnly = true)
    public List<StaffWorkloadDto> getStaffWorkload(String role) {
        List<StaffProfileJpa> staffList = role != null && !role.isBlank()
                ? staffProfileRepo.findAll().stream()
                    .filter(s -> s.getRoles() != null && s.getRoles().stream()
                            .anyMatch(r -> role.equalsIgnoreCase(r.getRoleCode())))
                    .toList()
                : staffProfileRepo.findAll();

        return staffList.stream()
                .map(this::buildWorkloadDto)
                .toList();
    }

    /**
     * Lấy workload của một staff cụ thể.
     */
    @Transactional(readOnly = true)
    public StaffWorkloadDto getStaffWorkload(Integer staffId) {
        StaffProfileJpa staff = staffProfileRepo.findByStaffId(staffId);
        if (staff == null) {
            throw new AssignmentException("Staff không tồn tại!", AssignmentErrorCode.UNAVAILABLE_STAFF);
        }
        return buildWorkloadDto(staff);
    }

    /**
     * Kiểm tra staff có thể assign thêm không.
     * ticketId = 0 nghĩa là kiểm tra chung, không liên quan ticket cụ thể.
     */
    @Transactional(readOnly = true)
    public boolean canAssignStaff(Integer staffId, Integer ticketId) {
        if (ticketId != null && ticketId > 0) {
            return ticketAssignmentRepo.isStaffAvailable(staffId)
                    && !ticketAssignmentRepo.isStaffAssignedToTicket(staffId, ticketId);
        }
        return ticketAssignmentRepo.isStaffAvailable(staffId);
    }

    /**
     * Kiểm tra ticket có thể chỉnh sửa assignment không.
     * Không cho phép khi phiếu đã COMPLETED, PAID hoặc CANCELLED.
     */
    private void requireTicketEditable(Integer ticketId) {
        ServiceTicket ticket = serviceTicketRepo.findByServiceTicketId(ticketId);
        if (ticket == null) {
            throw new AssignmentException("Không tìm thấy phiếu dịch vụ!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
        }
        TicketStatus status = ticket.getTicketStatus();
        if (status == TicketStatus.COMPLETED || status == TicketStatus.PAID || status == TicketStatus.CANCELLED) {
            throw new AssignmentException(
                "Không thể thay đổi phân công khi phiếu đã " + status,
                AssignmentErrorCode.INVALID_SERVICE_TICKET_ID
            );
        }
    }

    private StaffWorkloadDto buildWorkloadDto(StaffProfileJpa staff) {
        List<ServiceTicketAssignment> assignments = ticketAssignmentRepo.findByStaffId(staff.getStaffId());

        long active = assignments.stream()
                .filter(a -> AssignmentStatus.ACTIVE == a.getStatus())
                .count();
        long pending = assignments.stream()
                .filter(a -> AssignmentStatus.PENDING == a.getStatus())
                .count();

        boolean available = ticketAssignmentRepo.isStaffAvailable(staff.getStaffId());

        List<String> roles = staff.getRoles() == null ? List.of()
                : staff.getRoles().stream().map(r -> r.getRoleCode()).toList();

        List<WorkloadTicketDto> currentTickets = assignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACTIVE || a.getStatus() == AssignmentStatus.PENDING)
                .map(a -> {
                    WorkloadTicketDto t = new WorkloadTicketDto();
                    t.setRoleInTicket(a.getRoleInTicket());
                    t.setAssignmentStatus(a.getStatus() != null ? a.getStatus().name() : null);
                    t.setTicketCode(a.getTicketCode());
                    t.setTicketStatus(a.getTicketStatus() != null ? a.getTicketStatus().name() : null);
                    return t;
                })
                .toList();

        StaffWorkloadDto dto = new StaffWorkloadDto();
        dto.setStaffId(staff.getStaffId());
        dto.setFullName(staff.getFullName());
        dto.setPhone(staff.getPhone());
        dto.setRoles(roles);
        dto.setActiveAssignments((int) active);
        dto.setPendingAssignments((int) pending);
        dto.setTotalWorkload((int) (active + pending));
        dto.setIsAvailable(available);
        dto.setAvailabilityReason(available ? null : "Staff đang có assignment ACTIVE/PENDING");
        dto.setCurrentTickets(currentTickets);
        return dto;
    }
}



