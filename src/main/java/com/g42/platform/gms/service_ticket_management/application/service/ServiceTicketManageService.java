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
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.OdometerHistoryJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.VehicleConditionPhotoJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.OdometerHistoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.VehicleConditionPhotoRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.specification.ServiceTicketSpecification;
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
import org.springframework.data.jpa.domain.Specification;
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
    
    private final ServiceTicketRepository serviceTicketRepository;
    private final CustomerProfileRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final CatalogItemRepository catalogRepository;
    private final OdometerHistoryRepository odometerRepository;
    private final VehicleConditionPhotoRepository photoRepository;
    private final StaffProfileRepo staffRepository;
    private final ServiceTicketListMapper listMapper;
    private final ServiceTicketDetailMapper detailMapper;
    
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
        
        Specification<ServiceTicketJpa> specification = Specification.anyOf();
        specification = specification.and(ServiceTicketSpecification.filter(date, status));
        
        if (search != null && !search.isBlank()) {
            specification = specification.and(ServiceTicketSpecification.search(search));
        }
        
        Page<ServiceTicketJpa> ticketPage = serviceTicketRepository.findAll(specification, pageable);
        return ticketPage.map(this::mapToListResponse);
    }
    
    /**
     * Map ServiceTicketJpa to ServiceTicketListResponse using MapStruct.
     */
    private ServiceTicketListResponse mapToListResponse(ServiceTicketJpa ticket) {
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
        
        ServiceTicketJpa ticket = serviceTicketRepository.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));
        
        ServiceTicketDetailResponse response = new ServiceTicketDetailResponse();
        
        // Basic ticket info
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
        Optional<OdometerHistoryJpa> latestOdometer = odometerRepository.findLatestByVehicleId(ticket.getVehicleId());
        if (latestOdometer.isPresent()) {
            response.setOdometerReading(latestOdometer.get().getReading());
        }
        
        // Photos
        List<VehicleConditionPhotoJpa> photos = photoRepository.findAll().stream()
            .filter(photo -> photo.getServiceTicketId().equals(ticket.getServiceTicketId()))
            .toList();
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
     * Update service ticket (customer request and services).
     * Chỉ cho phép update khi ticket chưa COMPLETED hoặc CANCELLED.
     * 
     * @param ticketCode Ticket code
     * @param request Update request
     * @return Updated ServiceTicketDetailResponse
     */
    @Transactional
    public ServiceTicketDetailResponse updateServiceTicket(String ticketCode, UpdateServiceTicketRequest request) {
        log.info("Updating service ticket: {}", ticketCode);
        
        ServiceTicketJpa ticket = serviceTicketRepository.findByTicketCode(ticketCode)
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));
        
        if (ticket.getTicketStatus() == TicketStatus.COMPLETED) {
            throw new CheckInException("Không thể chỉnh sửa service ticket đã hoàn thành");
        }
        
        if (ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            throw new CheckInException("Không thể chỉnh sửa service ticket đã hủy");
        }
        
        if (request.getCustomerRequest() != null) {
            ticket.setCustomerRequest(request.getCustomerRequest());
        }
        
        if (ticket.getBookingId() != null && request.getCatalogItemIds() != null) {
            Booking booking = bookingRepository.findById(ticket.getBookingId())
                .orElseThrow(() -> new CheckInException("Không tìm thấy booking"));
            
            booking.setCatalogItemIds(request.getCatalogItemIds());
            bookingRepository.save(booking);
            
            log.info("Updated services for booking {}: {}", booking.getBookingId(), request.getCatalogItemIds());
        }
        
        serviceTicketRepository.save(ticket);
        
        log.info("Service ticket updated successfully: {}", ticketCode);
        return getServiceTicketDetail(ticketCode);
    }
}
