package com.g42.platform.gms.service_ticket_management.application.service;


import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.dashboard.application.service.StaffNotifyService;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.UpdateServiceTicketRequest;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;


/**
 * Service for advisor actions on service tickets.
 * Advisor quản lý luồng sửa chữa: start, wait-parts, resume, back-to-draft, cancel, update estimate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTicketAdvisorService {


    private final ServiceTicketRepo serviceTicketRepo;
    private final BookingRepository bookingRepository;
    private final ServiceTicketManageService manageService; // delegate getDetail/getList
    private final TicketAssignmentService ticketAssignmentService;
    private final StaffNotifyService staffNotifyService;


    /** Advisor duyệt báo giá, bắt đầu sửa — ESTIMATED/PENDING → REPAIRING.
     * Theo diagram: ESTIMATED → REPAIRING (Approved), PENDING → REPAIRING (Service/part available)
     */
    @Transactional
    public ServiceTicketDetailResponse startService(String ticketCode, Integer staffId) {
        ServiceTicket ticket = findTicket(ticketCode);
        TicketStatus current = ticket.getTicketStatus();
        if (current != TicketStatus.ESTIMATED && current != TicketStatus.PENDING) {
            throw new CheckInException("Chỉ có thể bắt đầu sửa khi phiếu đang ESTIMATED hoặc PENDING. Hiện tại: " + current);
        }
        ticket.setTicketStatus(TicketStatus.REPAIRING);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);

        ticketAssignmentService.startWork(ticket.getServiceTicketId(), staffId);

        log.info("Ticket {} → REPAIRING (advisor approved) from {} by staffId: {}", ticketCode, current, staffId);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Advisor lập báo giá — INSPECTED → ESTIMATED. */
    @Transactional
    public ServiceTicketDetailResponse createEstimate(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        requireStatus(ticket, TicketStatus.INSPECTED);
        ticket.setTicketStatus(TicketStatus.ESTIMATED);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → ESTIMATED", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Advisor báo chờ phụ tùng/dịch vụ không khả dụng — CREATED/ESTIMATED → PENDING. */
    @Transactional
    public ServiceTicketDetailResponse waitForParts(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        TicketStatus current = ticket.getTicketStatus();
        if (current != TicketStatus.CREATED && current != TicketStatus.ESTIMATED) {
            throw new CheckInException("Chỉ có thể chuyển PENDING từ CREATED hoặc ESTIMATED. Hiện tại: " + current);
        }
        ticket.setTicketStatus(TicketStatus.PENDING);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → PENDING (service/part not available)", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Advisor xác nhận có phụ tùng, tiếp tục sửa — PENDING → REPAIRING. */
    @Transactional
    public ServiceTicketDetailResponse resumeWork(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        requireStatus(ticket, TicketStatus.PENDING);
        ticket.setTicketStatus(TicketStatus.REPAIRING);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → REPAIRING (parts available)", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Khách yêu cầu thêm dịch vụ — REPAIRING → ESTIMATED (để advisor cập nhật báo giá). */
    @Transactional
    public ServiceTicketDetailResponse requestAddService(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        requireStatus(ticket, TicketStatus.REPAIRING);
        ticket.setTicketStatus(TicketStatus.ESTIMATED);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → ESTIMATED (customer request add service)", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Hủy phiếu — CREATED/PENDING/REPAIRING → CANCELLED. */
    @Transactional
    public ServiceTicketDetailResponse cancelTicket(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        TicketStatus current = ticket.getTicketStatus();
        boolean canCancel = current == TicketStatus.CREATED
                || current == TicketStatus.PENDING
                || current == TicketStatus.REPAIRING
                || current == TicketStatus.INSPECTING
                || current == TicketStatus.INSPECTED
                || current == TicketStatus.ESTIMATED;
        if (!canCancel) {
            throw new CheckInException("Không thể hủy phiếu ở trạng thái: " + current);
        }
        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → CANCELLED", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }


    /** Advisor cập nhật dịch vụ/báo giá khi ticket đang DRAFT. */
    @Transactional
    public ServiceTicketDetailResponse updateEstimate(String ticketCode, UpdateServiceTicketRequest request) {
        ServiceTicket ticket = findTicket(ticketCode);
        TicketStatus current = ticket.getTicketStatus();
        boolean isFinalized = current == TicketStatus.COMPLETED
                || current == TicketStatus.PAID
                || current == TicketStatus.CANCELLED;
        if (isFinalized) {
            throw new CheckInException("Không thể cập nhật báo giá khi phiếu đang " + current);
        }
        if (request.getCustomerRequest() != null) {
            ticket.setCustomerRequest(request.getCustomerRequest());
        }
        if (ticket.getBookingId() != null && request.getCatalogItemIds() != null) {
            Booking booking = bookingRepository.findById(ticket.getBookingId())
                    .orElseThrow(() -> new CheckInException("Không tìm thấy booking"));
            booking.setCatalogItemIds(request.getCatalogItemIds());
            bookingRepository.save(booking);
        }
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} estimate updated by advisor", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }


    /**
     * Advisor thay đổi advisor phụ trách ticket.
     * Chỉ được phép thay đổi khi advisor hiện tại đang ở trạng thái ACTIVE.
     */
    @Transactional
    public ServiceTicketDetailResponse changeAdvisor(String ticketCode, Integer newAdvisorId, String note) {
        log.info("Advisor changing advisor to {} for ticket: {}", newAdvisorId, ticketCode);

        ServiceTicket ticket = findTicket(ticketCode);
        ticketAssignmentService.changeAdvisorByAdvisor(ticket.getServiceTicketId(), newAdvisorId, note);

        log.info("Advisor changed successfully for ticket: {}", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }


    /**
     * Advisor hủy assignment technician.
     * Chỉ được phép hủy khi technician đang ở trạng thái PENDING.
     */
    @Transactional
    public ServiceTicketDetailResponse removeTechnician(String ticketCode, Integer technicianId) {
        log.info("Advisor removing technician {} from ticket: {}", technicianId, ticketCode);


        ServiceTicket ticket = findTicket(ticketCode);

        // Delegate to TicketAssignmentService for the actual logic
        ticketAssignmentService.removeTechnician(ticket.getServiceTicketId(), technicianId);


        log.info("Technician {} removed from ticket: {}", technicianId, ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }


    /**
     * Advisor thay đổi technician.
     * Hủy technician cũ và assign technician mới với trạng thái PENDING.
     */
    @Transactional
    public ServiceTicketDetailResponse changeTechnician(String ticketCode, Integer oldTechnicianId, Integer newTechnicianId, String note) {
        log.info("Advisor changing technician from {} to {} for ticket: {}", oldTechnicianId, newTechnicianId, ticketCode);


        ServiceTicket ticket = findTicket(ticketCode);

        // Delegate to TicketAssignmentService for the actual logic
        ticketAssignmentService.changeTechnician(ticket.getServiceTicketId(), oldTechnicianId, newTechnicianId, note);


        log.info("Technician changed successfully for ticket: {}", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }


    private ServiceTicket findTicket(String ticketCode) {
        return serviceTicketRepo.findByTicketCode(ticketCode)
                .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));
    }


    private void requireStatus(ServiceTicket ticket, TicketStatus required) {
        if (ticket.getTicketStatus() != required) {
            throw new CheckInException("Yêu cầu trạng thái " + required + ". Hiện tại: " + ticket.getTicketStatus());
        }
    }
}

