package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketListResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.UpdateServiceTicketRequest;
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
 * Service for managing service tickets (receptionist view).
 * Tương tự BookingManageService trong booking_management package.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTicketManageService {
    
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
    private final TicketAssignmentService ticketAssignmentService;
    
    /**
     * Get paginated list of service tickets with filters.
     * 
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param date Filter by received date
     * @param status Filter by ticket status
     * @param search Search by ticket code, customer name, phone, or license plate
     * @return Page of ServiceTicketListResponse
     */
    @Transactional(readOnly = true)
    public Page<ServiceTicketListResponse> getServiceTicketList(
            int page, 
            int size, 
            LocalDate date, 
            TicketStatus status, 
            String search) {
        
        log.info("Getting service ticket list: page={}, size={}, date={}, status={}, search={}", 
            page, size, date, status, search);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));

        Page<ServiceTicket> ticketPage = serviceTicketRepo.findAll(status, date, search, pageable);
        return ticketPage.map(this::mapToListResponse);
    }

    private ServiceTicketListResponse mapToListResponse(ServiceTicket ticket) {
        CustomerProfile customer = customerRepository.findById(ticket.getCustomerId()).orElse(null);
        Vehicle vehicle = vehicleRepository.findById(ticket.getVehicleId()).orElse(null);

        Booking booking = null;
        if (ticket.getBookingId() != null) {
            booking = bookingRepository.findById(ticket.getBookingId()).orElse(null);
        }

        return listMapper.toManageListResponse(ticket, customer, vehicle, booking);
    }
    
    @Transactional(readOnly = true)
    public ServiceTicketDetailResponse getServiceTicketDetail(String ticketCode) {
        log.info("Getting service ticket detail: {}", ticketCode);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        ServiceTicketDetailResponse response = new ServiceTicketDetailResponse();

        response.setServiceTicketId(ticket.getServiceTicketId());
        response.setTicketCode(ticket.getTicketCode());
        response.setTicketStatus(ticket.getTicketStatus());
        response.setCustomerRequest(ticket.getCustomerRequest());
        response.setCheckInNotes(ticket.getCheckInNotes());
        response.setReceivedAt(ticket.getReceivedAt());
        response.setDeliveredAt(ticket.getDeliveredAt());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
        response.setImmutable(ticket.getImmutable());
        response.setSafetyInspectionEnabled(ticket.getSafetyInspectionEnabled());
        
        // Customer info
        CustomerProfile customer = customerRepository.findById(ticket.getCustomerId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
        response.setCustomer(detailMapper.toManageCustomerInfo(customer));
        
        // Vehicle info
        Vehicle vehicle = vehicleRepository.findById(ticket.getVehicleId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy xe"));
        response.setVehicle(detailMapper.toManageVehicleInfo(vehicle));
        
        // Booking info
        if (ticket.getBookingId() != null) {
            Booking booking = bookingRepository.findById(ticket.getBookingId()).orElse(null);
            if (booking != null) {
                response.setBooking(detailMapper.toManageBookingInfo(booking));
                response.setServiceCategory(booking.getServiceCategory());
                response.setIsGuest(booking.getIsGuest());
                
                // Services
                if (booking.getCatalogItemIds() != null) {
                    List<com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity> catalogItems = 
                        catalogRepository.findAllById(booking.getCatalogItemIds());
                    response.setServices(detailMapper.toManageServiceInfoList(catalogItems));
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
        response.setPhotos(detailMapper.toManagePhotoInfoList(photos));

        // Staff info
        if (ticket.getCreatedBy() != null) {
            response.setCreatedBy(ticket.getCreatedBy());
            Optional<StaffProfile> staff = staffRepository.findById(ticket.getCreatedBy());
            if (staff.isPresent()) {
                response.setCreatedByName(staff.get().getFullName());
            }
        }

        log.info("Service ticket detail retrieved: {}", ticketCode);
        return response;
    }

    /**
     * Lễ tân xác nhận thanh toán — COMPLETED → PAID.
     * TODO: implement phần thanh toán + ZNS trigger sau.
     */
    @Transactional
    public ServiceTicketDetailResponse completeTicket(String ticketCode) {
        log.info("Receptionist completing ticket: {}", ticketCode);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        if (ticket.getTicketStatus() != TicketStatus.COMPLETED) {
            throw new CheckInException("Chỉ có thể xác nhận thanh toán khi phiếu đang COMPLETED. Hiện tại: " + ticket.getTicketStatus());
        }

        ticket.setTicketStatus(TicketStatus.PAID);
        ticket.setDeliveredAt(java.time.LocalDateTime.now());
        ticket.setUpdatedAt(java.time.LocalDateTime.now());
        serviceTicketRepo.save(ticket);

        // Mark all assignments as DONE when ticket is paid
        markAssignmentsDone(ticket.getServiceTicketId());

        // TODO: trigger ZNS feedback notification here
        log.info("Ticket {} → PAID", ticketCode);
        return getServiceTicketDetail(ticketCode);
    }

    /**
     * Lễ tân thay đổi advisor cho ticket.
     * Chỉ được phép thay đổi khi advisor hiện tại đang ở trạng thái PENDING (chưa bắt đầu làm việc).
     */
    @Transactional
    public ServiceTicketDetailResponse changeAdvisor(String ticketCode, Integer newAdvisorId, String note) {
        log.info("Receptionist changing advisor for ticket: {} to staffId: {}", ticketCode, newAdvisorId);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        // Delegate to TicketAssignmentService for the actual logic
        ticketAssignmentService.changeAdvisor(ticket.getServiceTicketId(), newAdvisorId, note);

        log.info("Advisor changed successfully for ticket: {}", ticketCode);
        return getServiceTicketDetail(ticketCode);
    }

    /**
     * Đánh dấu assignments hoàn thành khi ticket được thanh toán.
     */
    @Transactional
    public void markAssignmentsDone(Integer ticketId) {
        ticketAssignmentService.markAssignmentDone(ticketId);
    }

}
