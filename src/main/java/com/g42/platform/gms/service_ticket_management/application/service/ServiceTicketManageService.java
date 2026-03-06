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
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.OdometerHistoryJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.VehicleConditionPhotoJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.OdometerHistoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.VehicleConditionPhotoRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.specification.ServiceTicketSpecification;
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
        
        // === 1. TẠO PAGEABLE VỚI SORT ===
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        
        // === 2. TẠO SPECIFICATION CHO FILTER ===
        Specification<ServiceTicketJpa> specification = Specification.anyOf();
        
        // Áp dụng filter theo date và status
        specification = specification.and(ServiceTicketSpecification.filter(date, status));
        
        // === 3. ÁP DỤNG SEARCH NẾU CÓ ===
        if (search != null && !search.isBlank()) {
            specification = specification.and(ServiceTicketSpecification.search(search));
        }
        
        // === 4. QUERY VỚI SPECIFICATION ===
        Page<ServiceTicketJpa> ticketPage = serviceTicketRepository.findAll(specification, pageable);
        
        // === 5. MAP TO RESPONSE DTO ===
        return ticketPage.map(this::mapToListResponse);
    }
    
    /**
     * Map ServiceTicketJpa to ServiceTicketListResponse.
     */
    private ServiceTicketListResponse mapToListResponse(ServiceTicketJpa ticket) {
        ServiceTicketListResponse response = new ServiceTicketListResponse();
        
        // Basic ticket info
        response.setServiceTicketId(ticket.getServiceTicketId());
        response.setTicketCode(ticket.getTicketCode());
        response.setTicketStatus(ticket.getTicketStatus());
        response.setReceivedAt(ticket.getReceivedAt());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setCustomerRequest(ticket.getCustomerRequest());
        
        // Customer info
        Optional<CustomerProfile> customer = customerRepository.findById(ticket.getCustomerId());
        if (customer.isPresent()) {
            response.setCustomerId(customer.get().getCustomerId());
            response.setCustomerName(customer.get().getFullName());
            response.setCustomerPhone(customer.get().getPhone());
        }
        
        // Vehicle info
        Optional<Vehicle> vehicle = vehicleRepository.findById(ticket.getVehicleId());
        if (vehicle.isPresent()) {
            response.setVehicleId(vehicle.get().getVehicleId());
            response.setLicensePlate(vehicle.get().getLicensePlate());
            response.setVehicleMake(vehicle.get().getBrand());
            response.setVehicleModel(vehicle.get().getModel());
        }
        
        // Booking info
        if (ticket.getBookingId() != null) {
            Optional<Booking> booking = bookingRepository.findById(ticket.getBookingId());
            if (booking.isPresent()) {
                response.setBookingId(booking.get().getBookingId());
                response.setBookingCode(booking.get().getBookingCode());
                response.setScheduledDate(booking.get().getScheduledDate());
                response.setScheduledTime(booking.get().getScheduledTime());
                response.setServiceCategory(booking.get().getServiceCategory());
                response.setIsGuest(booking.get().getIsGuest());
            }
        }
        
        return response;
    }
    
    /**
     * Get service ticket detail by ticket code.
     * 
     * @param ticketCode Ticket code (ST_XXXXXX or MST_XXXXXX)
     * @return ServiceTicketDetailResponse with full information
     */
    @Transactional(readOnly = true)
    public ServiceTicketDetailResponse getServiceTicketDetail(String ticketCode) {
        log.info("Getting service ticket detail: {}", ticketCode);
        
        // Find ticket
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
        
        // Customer info
        CustomerProfile customer = customerRepository.findById(ticket.getCustomerId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
        
        ServiceTicketDetailResponse.CustomerInfo customerInfo = new ServiceTicketDetailResponse.CustomerInfo();
        customerInfo.setCustomerId(customer.getCustomerId());
        customerInfo.setFullName(customer.getFullName());
        customerInfo.setPhone(customer.getPhone());
        customerInfo.setEmail(customer.getEmail());
        response.setCustomer(customerInfo);
        
        // Vehicle info
        Vehicle vehicle = vehicleRepository.findById(ticket.getVehicleId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy xe"));
        
        ServiceTicketDetailResponse.VehicleInfo vehicleInfo = new ServiceTicketDetailResponse.VehicleInfo();
        vehicleInfo.setVehicleId(vehicle.getVehicleId());
        vehicleInfo.setLicensePlate(vehicle.getLicensePlate());
        vehicleInfo.setMake(vehicle.getBrand());
        vehicleInfo.setModel(vehicle.getModel());
        vehicleInfo.setYear(vehicle.getManufactureYear());
        response.setVehicle(vehicleInfo);
        
        // Booking info
        if (ticket.getBookingId() != null) {
            Optional<Booking> booking = bookingRepository.findById(ticket.getBookingId());
            if (booking.isPresent()) {
                ServiceTicketDetailResponse.BookingInfo bookingInfo = new ServiceTicketDetailResponse.BookingInfo();
                bookingInfo.setBookingId(booking.get().getBookingId());
                bookingInfo.setBookingCode(booking.get().getBookingCode());
                bookingInfo.setScheduledDate(booking.get().getScheduledDate());
                bookingInfo.setScheduledTime(booking.get().getScheduledTime());
                response.setBooking(bookingInfo);
                
                response.setServiceCategory(booking.get().getServiceCategory());
                response.setIsGuest(booking.get().getIsGuest());
                
                // Services from booking
                List<ServiceTicketDetailResponse.ServiceInfo> services = new ArrayList<>();
                if (booking.get().getServiceIds() != null) {
                    for (Integer serviceId : booking.get().getServiceIds()) {
                        Optional<com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity> catalogItem = 
                            catalogRepository.findById(serviceId);
                        if (catalogItem.isPresent()) {
                            ServiceTicketDetailResponse.ServiceInfo serviceInfo = new ServiceTicketDetailResponse.ServiceInfo();
                            serviceInfo.setServiceId(catalogItem.get().getItemId());
                            serviceInfo.setServiceName(catalogItem.get().getItemName());
                            serviceInfo.setCategory(catalogItem.get().getItemType());
                            services.add(serviceInfo);
                        }
                    }
                }
                response.setServices(services);
            }
        }
        
        // Odometer reading
        Optional<OdometerHistoryJpa> latestOdometer = odometerRepository.findLatestByVehicleId(ticket.getVehicleId());
        if (latestOdometer.isPresent()) {
            response.setOdometerReading(latestOdometer.get().getReading());
        }
        
        // Photos
        List<VehicleConditionPhotoJpa> allPhotos = photoRepository.findAll();
        List<ServiceTicketDetailResponse.PhotoInfo> photoInfos = new ArrayList<>();
        for (VehicleConditionPhotoJpa photo : allPhotos) {
            if (photo.getServiceTicketId().equals(ticket.getServiceTicketId())) {
                ServiceTicketDetailResponse.PhotoInfo photoInfo = new ServiceTicketDetailResponse.PhotoInfo();
                photoInfo.setPhotoId(photo.getPhotoId());
                photoInfo.setCategory(photo.getCategory().name());
                photoInfo.setPhotoUrl(photo.getPhotoUrl());
                photoInfo.setDescription(photo.getDescription());
                photoInfo.setUploadedAt(photo.getUploadedAt());
                photoInfos.add(photoInfo);
            }
        }
        response.setPhotos(photoInfos);
        
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
}
