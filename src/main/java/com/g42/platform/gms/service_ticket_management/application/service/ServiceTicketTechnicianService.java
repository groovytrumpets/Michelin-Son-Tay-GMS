package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import com.g42.platform.gms.service_ticket_management.api.dto.technician.TechnicianTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.technician.TechnicianTicketListResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.technician.UpdateTechnicianNotesRequest;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.service_ticket_management.domain.repository.OdometerReadingRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.VehicleConditionPhotoRepo;
import com.g42.platform.gms.service_ticket_management.api.mapper.ServiceTicketListMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.ServiceTicketDetailMapper;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for technician service ticket management.
 * Quản lý phiếu dịch vụ cho kỹ thuật viên.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTicketTechnicianService {
    
    private final ServiceTicketRepo serviceTicketRepo;
    private final CustomerProfileRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final CatalogItemRepository catalogRepository;
    private final OdometerReadingRepo odometerRepo;
    private final VehicleConditionPhotoRepo photoRepo;
    private final StaffProfileRepo staffRepository;
    private final ServiceTicketListMapper listMapper;
    private final ServiceTicketDetailMapper detailMapper;
    
    /**
     * Get paginated list of service tickets assigned to a specific technician.
     * 
     * @param staffId  ID của kỹ thuật viên (lấy từ JWT)
     * @param page     Page number (0-indexed)
     * @param size     Page size
     * @param date     Filter by received date
     * @param status   Filter by ticket status
     * @param search   Search by ticket code, customer name, phone, or license plate
     * @return Page of TechnicianTicketListResponse
     */
    @Transactional(readOnly = true)
    public Page<TechnicianTicketListResponse> getTechnicianTicketList(
            Integer staffId,
            int page, 
            int size, 
            LocalDate date, 
            TicketStatus status, 
            String search) {
        
        log.info("Getting technician ticket list: staffId={}, page={}, size={}, date={}, status={}, search={}", 
            staffId, page, size, date, status, search);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        
        Page<ServiceTicket> ticketPage = serviceTicketRepo.findByAssignedStaff(staffId, status, date, search, pageable);
        return ticketPage.map(this::mapToListResponse);
    }

    private TechnicianTicketListResponse mapToListResponse(ServiceTicket ticket) {
        CustomerProfile customer = customerRepository.findById(ticket.getCustomerId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(ticket.getVehicleId()).orElse(null);

        Booking booking = null;
        if (ticket.getBookingId() != null) {
            booking = bookingRepository.findById(ticket.getBookingId()).orElse(null);
        }

        return listMapper.toTechnicianListResponse(ticket, customer, vehicle, booking);
    }
    
    /**
     * Get service ticket detail for technician.
     * 
     * @param ticketCode Ticket code (ST_XXXXXX or MST_XXXXXX)
     * @return TechnicianTicketDetailResponse with full information
     */
    @Transactional(readOnly = true)
    public TechnicianTicketDetailResponse getTechnicianTicketDetail(String ticketCode) {
        log.info("Getting technician ticket detail: {}", ticketCode);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        TechnicianTicketDetailResponse response = new TechnicianTicketDetailResponse();

        response.setServiceTicketId(ticket.getServiceTicketId());
        response.setTicketCode(ticket.getTicketCode());
        response.setTicketStatus(ticket.getTicketStatus());
        response.setCustomerRequest(ticket.getCustomerRequest());
        response.setTechnicianNotes(ticket.getTechnicianNotes());
        response.setCheckInNotes(ticket.getCheckInNotes());
        response.setReceivedAt(ticket.getReceivedAt());
        response.setDeliveredAt(ticket.getDeliveredAt());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        
        // Customer info
        CustomerProfile customer = customerRepository.findById(ticket.getCustomerId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
        response.setCustomer(detailMapper.toTechnicianCustomerInfo(customer));
        
        // Vehicle info
        Vehicle vehicle = vehicleRepository.findById(ticket.getVehicleId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy xe"));
        response.setVehicle(detailMapper.toTechnicianVehicleInfo(vehicle));
        
        // Booking info
        if (ticket.getBookingId() != null) {
            Booking booking = bookingRepository.findById(ticket.getBookingId()).orElse(null);
            if (booking != null) {
                response.setBooking(detailMapper.toTechnicianBookingInfo(booking));
                response.setServiceCategory(booking.getServiceCategory());
                
                // Services
                if (booking.getCatalogItemIds() != null) {
                    List<com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity> catalogItems = 
                        catalogRepository.findAllById(booking.getCatalogItemIds());
                    response.setServices(detailMapper.toTechnicianServiceInfoList(catalogItems));
                }
            }
        }
        
        // Odometer
        Optional<OdometerReading> latestOdometer = odometerRepo.findLatestByVehicleId(ticket.getVehicleId());
        if (latestOdometer.isPresent()) {
            response.setOdometerReading(latestOdometer.get().getReading());
        }
        
        // Photos
        List<VehicleConditionPhoto> photos = photoRepo.findByServiceTicketId(ticket.getServiceTicketId());
        response.setPhotos(detailMapper.toTechnicianPhotoInfoList(photos));

        // Staff info
        if (ticket.getCreatedBy() != null) {
            response.setCreatedBy(ticket.getCreatedBy());
            Optional<StaffProfile> staff = staffRepository.findById(ticket.getCreatedBy());
            if (staff.isPresent()) {
                response.setCreatedByName(staff.get().getFullName());
            }
        }

        log.info("Technician ticket detail retrieved: {}", ticketCode);
        return response;
    }

    @Transactional
    public TechnicianTicketDetailResponse updateTechnicianNotes(String ticketCode, UpdateTechnicianNotesRequest request) {
        log.info("Updating technician notes for ticket: {}", ticketCode);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        if (ticket.getTicketStatus() == TicketStatus.COMPLETED
                || ticket.getTicketStatus() == TicketStatus.PAID) {
            throw new CheckInException("Không thể chỉnh sửa service ticket đã hoàn thành");
        }

        if (ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            throw new CheckInException("Không thể chỉnh sửa service ticket đã hủy");
        }

        ticket.setTechnicianNotes(request.getTechnicianNotes());
        serviceTicketRepo.save(ticket);

        log.info("Technician notes updated successfully: {}", ticketCode);
        return getTechnicianTicketDetail(ticketCode);
    }

    /**
     * Technician bắt đầu sửa xe — PENDING → IN_PROGRESS.
     */
    @Transactional
    public TechnicianTicketDetailResponse startWork(String ticketCode) {
        log.info("Technician starting work on ticket: {}", ticketCode);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        if (ticket.getTicketStatus() != TicketStatus.PENDING) {
            throw new CheckInException("Chỉ có thể bắt đầu sửa khi phiếu đang ở trạng thái PENDING");
        }

        ticket.setTicketStatus(TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(java.time.LocalDateTime.now());
        serviceTicketRepo.save(ticket);

        log.info("Ticket {} moved to IN_PROGRESS", ticketCode);
        return getTechnicianTicketDetail(ticketCode);
    }

    /**
     * Technician báo thiếu phụ tùng, cần chờ — IN_PROGRESS → PENDING.
     */
    @Transactional
    public TechnicianTicketDetailResponse waitForParts(String ticketCode,
            com.g42.platform.gms.service_ticket_management.api.dto.technician.WaitPartsRequest request) {
        log.info("Technician waiting for parts on ticket: {}", ticketCode);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        if (ticket.getTicketStatus() != TicketStatus.IN_PROGRESS) {
            throw new CheckInException("Chỉ có thể báo chờ phụ tùng khi phiếu đang ở trạng thái IN_PROGRESS");
        }

        ticket.setTicketStatus(TicketStatus.PENDING);
        ticket.setUpdatedAt(java.time.LocalDateTime.now());
        if (request != null && request.getReason() != null) {
            String notes = ticket.getTechnicianNotes() != null ? ticket.getTechnicianNotes() : "";
            ticket.setTechnicianNotes(notes + "\n[Chờ phụ tùng] " + request.getReason());
        }
        serviceTicketRepo.save(ticket);

        log.info("Ticket {} moved back to PENDING (waiting for parts)", ticketCode);
        return getTechnicianTicketDetail(ticketCode);
    }

    /**
     * Technician báo xong sửa xe — IN_PROGRESS → COMPLETED.
     * Chỉ là signal "xe xong", lễ tân sẽ xác nhận thanh toán sau.
     */
    @Transactional
    public TechnicianTicketDetailResponse finishWork(String ticketCode) {
        log.info("Technician finishing work on ticket: {}", ticketCode);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        if (ticket.getTicketStatus() != TicketStatus.IN_PROGRESS) {
            throw new CheckInException("Chỉ có thể báo hoàn thành khi phiếu đang ở trạng thái IN_PROGRESS");
        }

        ticket.setTicketStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(java.time.LocalDateTime.now());
        ticket.setUpdatedAt(java.time.LocalDateTime.now());
        serviceTicketRepo.save(ticket);

        log.info("Ticket {} marked as COMPLETED by technician", ticketCode);
        return getTechnicianTicketDetail(ticketCode);
    }

}
