package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
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

    /** Advisor bắt đầu dịch vụ sửa — DRAFT → IN_PROGRESS. */
    @Transactional
    public ServiceTicketDetailResponse startService(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        requireStatus(ticket, TicketStatus.DRAFT);
        ticket.setTicketStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → IN_PROGRESS (advisor start)", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Advisor báo chờ phụ tùng — IN_PROGRESS → PENDING. */
    @Transactional
    public ServiceTicketDetailResponse waitForParts(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        requireStatus(ticket, TicketStatus.IN_PROGRESS);
        ticket.setTicketStatus(TicketStatus.PENDING);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → PENDING (waiting for parts)", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Advisor xác nhận có phụ tùng, tiếp tục sửa — PENDING → IN_PROGRESS. */
    @Transactional
    public ServiceTicketDetailResponse resumeWork(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        requireStatus(ticket, TicketStatus.PENDING);
        ticket.setTicketStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → IN_PROGRESS (parts available)", ticketCode);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Advisor đưa về DRAFT để thêm dịch vụ vào báo giá — PENDING/IN_PROGRESS → DRAFT. */
    @Transactional
    public ServiceTicketDetailResponse backToDraft(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        TicketStatus current = ticket.getTicketStatus();
        if (current != TicketStatus.PENDING && current != TicketStatus.IN_PROGRESS) {
            throw new CheckInException("Chỉ có thể đưa về DRAFT khi phiếu đang PENDING hoặc IN_PROGRESS. Hiện tại: " + current);
        }
        ticket.setTicketStatus(TicketStatus.DRAFT);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → DRAFT from {} (add new estimate item)", ticketCode, current);
        return manageService.getServiceTicketDetail(ticketCode);
    }

    /** Hủy phiếu — DRAFT/PENDING/IN_PROGRESS → CANCELLED. */
    @Transactional
    public ServiceTicketDetailResponse cancelTicket(String ticketCode) {
        ServiceTicket ticket = findTicket(ticketCode);
        TicketStatus current = ticket.getTicketStatus();
        if (current != TicketStatus.DRAFT && current != TicketStatus.PENDING && current != TicketStatus.IN_PROGRESS) {
            throw new CheckInException("Chỉ có thể hủy khi phiếu đang DRAFT, PENDING hoặc IN_PROGRESS. Hiện tại: " + current);
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
        if (current == TicketStatus.COMPLETED || current == TicketStatus.PAID || current == TicketStatus.CANCELLED) {
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
