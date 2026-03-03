package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerAuth;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.auth.repository.CustomerAuthRepository;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.application.service.BookingService;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import com.g42.platform.gms.common.constant.FileUploadConstants;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.service_ticket_management.api.dto.checkin.*;
import com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;
import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.OdometerHistoryJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.VehicleConditionPhotoJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.OdometerReadingMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.ServiceTicketMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.VehicleConditionPhotoMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.OdometerHistoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.VehicleConditionPhotoRepository;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Check-in process.
 * Handles business logic for vehicle check-in (first step of service ticket lifecycle).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final ServiceTicketService serviceTicketService;
    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceTicketMapper serviceTicketMapper;
    private final ServiceTicketCodeGenerator ticketCodeGenerator;
    private final VehicleRepository vehicleRepository;
    private final CustomerProfileRepository customerRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final VehicleConditionPhotoRepository photoRepository;
    private final OdometerHistoryRepository odometerRepository;
    private final CatalogItemRepository catalogRepository;
    private final ImageUploadService imageUploadService;
    private final VehicleConditionPhotoMapper photoMapper;
    private final OdometerReadingMapper odometerMapper;
    private final com.g42.platform.gms.service_ticket_management.api.mapper.ServiceTicketDtoMapper serviceTicketDtoMapper;

    /**
     * Lookup booking by booking code.
     * Returns booking information with customer details and vehicle suggestions.
     * 
     * @param request BookingLookupRequest containing booking code
     * @return BookingLookupResponse with booking and customer information
     */
    @Transactional(readOnly = true)
    public BookingLookupResponse lookupBooking(BookingLookupRequest request) {
        log.info("Looking up booking: {}", request.getBookingCode());
        
        // 1. Query booking using BookingService (reuse existing logic)
        Booking booking = bookingService.findByCode(request.getBookingCode());
        
        // 2. Fetch customer information
        CustomerProfile customer = customerRepository.findById(booking.getCustomerId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy thông tin khách hàng"));
        
        // 3. Fetch services information
        List<BookingLookupResponse.ServiceInfo> services = new ArrayList<>();
        if (booking.getServiceIds() != null && !booking.getServiceIds().isEmpty()) {
            for (Integer serviceId : booking.getServiceIds()) {
                Optional<com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity> catalogItem = 
                    catalogRepository.findById(serviceId);
                if (catalogItem.isPresent()) {
                    BookingLookupResponse.ServiceInfo serviceInfo = new BookingLookupResponse.ServiceInfo();
                    serviceInfo.setServiceId(catalogItem.get().getItemId());
                    serviceInfo.setServiceName(catalogItem.get().getItemName());
                    serviceInfo.setCategory(catalogItem.get().getItemType()); // Use itemType instead of category
                    services.add(serviceInfo);
                }
            }
        }
        
        // 4. Map to BookingLookupResponse (without vehicleSuggestions)
        BookingLookupResponse response = new BookingLookupResponse();
        response.setBookingId(booking.getBookingId());
        response.setBookingCode(booking.getBookingCode());
        response.setScheduledDate(booking.getScheduledDate());
        response.setScheduledTime(booking.getScheduledTime());
        response.setServiceCategory(booking.getServiceCategory());
        response.setDescription(booking.getDescription());
        response.setCustomerId(customer.getCustomerId());
        response.setCustomerName(customer.getFullName());
        response.setCustomerPhone(customer.getPhone());
        response.setCustomerEmail(customer.getEmail());
        response.setServices(services);
        
        log.info("Booking lookup successful: bookingId={}, customerId={}", booking.getBookingId(), customer.getCustomerId());
        return response;
    }

    /**
     * Save or select vehicle for check-in.
     * If vehicleId provided, validate and return existing vehicle.
     * If vehicleId null, create new vehicle.
     * Automatically creates Service Ticket if not exists for this booking.
     * 
     * @param request VehicleRequest with vehicle data
     * @return VehicleResponse with vehicle information and ticket code
     */
    @Transactional
    public VehicleResponse saveVehicle(VehicleRequest request) {
        log.info("Saving vehicle - vehicleId: {}, licensePlate: {}", request.getVehicleId(), request.getLicensePlate());
        
        Vehicle vehicle;
        boolean isNewVehicle = false;
        
        // 1. If vehicleId provided, validate and return existing
        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new CheckInException("Không tìm thấy xe với ID: " + request.getVehicleId()));
            log.info("Using existing vehicle: vehicleId={}", vehicle.getVehicleId());
        } else {
            // 2. If vehicleId null, create new vehicle
            // Validate required fields for new vehicle
            if (request.getLicensePlate() == null || request.getLicensePlate().trim().isEmpty()) {
                throw new CheckInException("Biển số xe là bắt buộc khi tạo xe mới");
            }
            if (request.getMake() == null || request.getMake().trim().isEmpty()) {
                throw new CheckInException("Hãng xe là bắt buộc khi tạo xe mới");
            }
            if (request.getModel() == null || request.getModel().trim().isEmpty()) {
                throw new CheckInException("Model xe là bắt buộc khi tạo xe mới");
            }
            if (request.getYear() == null) {
                throw new CheckInException("Năm sản xuất là bắt buộc khi tạo xe mới");
            }
            
            // 3. Validate license plate uniqueness
            Optional<Vehicle> existingVehicle = vehicleRepository.findByLicensePlate(request.getLicensePlate());
            if (existingVehicle.isPresent()) {
                throw new CheckInException("Biển số xe đã tồn tại: " + request.getLicensePlate());
            }
            
            vehicle = new Vehicle();
            vehicle.setLicensePlate(request.getLicensePlate());
            vehicle.setBrand(request.getMake());
            vehicle.setModel(request.getModel());
            vehicle.setManufactureYear(request.getYear());
            
            // 4. Link vehicle to customer
            CustomerProfile customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
            vehicle.setCustomer(customer);
            
            vehicle = vehicleRepository.save(vehicle);
            isNewVehicle = true;
            log.info("Created new vehicle: vehicleId={}, licensePlate={}", vehicle.getVehicleId(), vehicle.getLicensePlate());
        }
        
        // 5. Check if Service Ticket already exists for this booking
        ServiceTicket serviceTicket;
        Optional<ServiceTicketJpa> existingTicket = serviceTicketRepository.findByBookingId(request.getBookingId());
        
        if (existingTicket.isPresent()) {
            // Ticket already exists - return existing ticket
            serviceTicket = serviceTicketMapper.toDomain(existingTicket.get());
            log.info("Service ticket already exists for booking: {}, ticketCode: {}", 
                request.getBookingId(), serviceTicket.getTicketCode());
            
            // Update vehicle_id if changed
            if (!serviceTicket.getVehicleId().equals(vehicle.getVehicleId())) {
                ServiceTicketJpa ticketJpa = existingTicket.get();
                ticketJpa.setVehicleId(vehicle.getVehicleId());
                ticketJpa.setUpdatedAt(LocalDateTime.now());
                serviceTicketRepository.save(ticketJpa);
                log.info("Updated vehicle_id in existing ticket: {} -> {}", 
                    serviceTicket.getVehicleId(), vehicle.getVehicleId());
                serviceTicket.setVehicleId(vehicle.getVehicleId());
            }
        } else {
            // Create new Service Ticket
            serviceTicket = serviceTicketService.createServiceTicket(
                request.getBookingId(),
                vehicle.getVehicleId(),
                request.getCustomerId()
            );
            log.info("Created new service ticket: ticketCode={}", serviceTicket.getTicketCode());
        }
        
        // 6. Map to VehicleResponse
        VehicleResponse response = new VehicleResponse();
        response.setVehicleId(vehicle.getVehicleId());
        response.setLicensePlate(vehicle.getLicensePlate());
        response.setMake(vehicle.getBrand());
        response.setModel(vehicle.getModel());
        response.setYear(vehicle.getManufactureYear());
        response.setCustomerId(vehicle.getCustomer() != null ? vehicle.getCustomer().getCustomerId() : null);
        response.setIsNewVehicle(isNewVehicle);
        response.setTicketCode(serviceTicket.getTicketCode());
        
        return response;
    }

    /**
     * Upload license plate photo.
     * 
     * @param file MultipartFile containing photo
     * @param vehicleId Vehicle ID
     * @return PhotoUploadResponse with photo URL
     */
    @Transactional
    public PhotoUploadResponse uploadLicensePlatePhoto(MultipartFile file, Integer vehicleId) {
        log.info("Uploading license plate photo for vehicle: {}", vehicleId);
        
        // 1. Upload to Cloudinary
        String photoUrl;
        try {
            photoUrl = imageUploadService.uploadImage(file, FileUploadConstants.FOLDER_VEHICLE);
        } catch (IOException e) {
            log.error("Failed to upload license plate photo", e);
            throw new CheckInException("Không thể upload ảnh biển số: " + e.getMessage());
        }
        
        // 2. Update vehicle.licensePlatePhotoUrl (Note: Vehicle entity doesn't have this field yet)
        // For now, we'll just return the URL - the field will be added to Vehicle entity later
        
        // 3. Return PhotoUploadResponse
        PhotoUploadResponse response = new PhotoUploadResponse();
        response.setPhotoUrl(photoUrl);
        response.setCategory(PhotoCategory.ODOMETER); // License plate is special category
        response.setUploadedAt(LocalDateTime.now());
        response.setMessage("Upload ảnh biển số thành công");
        
        log.info("License plate photo uploaded successfully: {}", photoUrl);
        return response;
    }

    /**
     * Upload vehicle condition photo.
     * 
     * @param file MultipartFile containing photo
     * @param request PhotoUploadRequest with category and description
     * @return PhotoUploadResponse with photo information
     */
    @Transactional
    public PhotoUploadResponse uploadConditionPhoto(MultipartFile file, PhotoUploadRequest request) {
        log.info("Uploading condition photo: category={}, ticketCode={}", 
                 request.getCategory(), request.getTicketCode());
        
        // 1. Upload to Cloudinary
        String photoUrl;
        try {
            photoUrl = imageUploadService.uploadImage(file, FileUploadConstants.FOLDER_VEHICLE);
        } catch (IOException e) {
            log.error("Failed to upload condition photo", e);
            throw new CheckInException("Không thể upload ảnh: " + e.getMessage());
        }
        
        // 2. Find service ticket
        ServiceTicket ticket = serviceTicketService.findByTicketCode(request.getTicketCode());
        ServiceTicketJpa ticketJpa = serviceTicketRepository.findByTicketCode(request.getTicketCode())
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket"));
        
        // 3. Create VehicleConditionPhotoJpa entity
        VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
        photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId()); // Use ID instead of object
        photoJpa.setCategory(request.getCategory());
        photoJpa.setPhotoUrl(photoUrl);
        photoJpa.setDescription(request.getDescription());
        photoJpa.setUploadedAt(LocalDateTime.now());
        photoJpa.setUploadedBy(request.getUploadedBy());
        
        // 4. Save to database
        photoJpa = photoRepository.save(photoJpa);
        
        // 5. Return PhotoUploadResponse
        PhotoUploadResponse response = new PhotoUploadResponse();
        response.setPhotoId(photoJpa.getPhotoId());
        response.setPhotoUrl(photoJpa.getPhotoUrl());
        response.setCategory(photoJpa.getCategory());
        response.setDescription(photoJpa.getDescription());
        response.setUploadedAt(photoJpa.getUploadedAt());
        response.setMessage("Upload ảnh thành công");
        
        log.info("Condition photo uploaded successfully: photoId={}, category={}", photoJpa.getPhotoId(), photoJpa.getCategory());
        return response;
    }

    /**
     * Save odometer reading with rollback detection.
     * 
     * @param request OdometerRequest with reading data
     * @return OdometerResponse with warning if rollback detected
     */
    @Transactional
    public OdometerResponse saveOdometer(OdometerRequest request) {
        log.info("Saving odometer reading: {} km for vehicle: {}", 
                 request.getReading(), request.getVehicleId());
        
        // 1. Query previous odometer reading
        Optional<OdometerHistoryJpa> previousReading = odometerRepository.findLatestByVehicleId(request.getVehicleId());
        
        // 2. Detect rollback (new < previous)
        boolean rollbackDetected = false;
        Integer previousReadingValue = null;
        String warningMessage = null;
        
        if (previousReading.isPresent()) {
            previousReadingValue = previousReading.get().getReading();
            if (request.getReading() < previousReadingValue) {
                rollbackDetected = true;
                warningMessage = String.format("CẢNH BÁO: Số công tơ mét hiện tại (%d km) nhỏ hơn lần trước (%d km). Có thể đã bị lùi công tơ mét.", 
                    request.getReading(), previousReadingValue);
                log.warn("Odometer rollback detected: vehicleId={}, previous={}, current={}", 
                    request.getVehicleId(), previousReadingValue, request.getReading());
            }
        }
        
        // 3. Find service ticket
        ServiceTicketJpa ticketJpa = serviceTicketRepository.findByTicketCode(request.getTicketCode())
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket"));
        
        // 4. Create OdometerHistoryJpa entity
        OdometerHistoryJpa odometerJpa = new OdometerHistoryJpa();
        odometerJpa.setVehicleId(request.getVehicleId());
        odometerJpa.setServiceTicketId(ticketJpa.getServiceTicketId()); // Use ID instead of object
        odometerJpa.setReading(request.getReading());
        odometerJpa.setRecordedAt(LocalDateTime.now());
        
        // 5. Save to database
        odometerJpa = odometerRepository.save(odometerJpa);
        
        // 6. Return OdometerResponse with warning if rollback
        OdometerResponse response = new OdometerResponse();
        response.setReadingId(odometerJpa.getReadingId());
        response.setVehicleId(odometerJpa.getVehicleId());
        response.setReading(odometerJpa.getReading());
        response.setRecordedAt(odometerJpa.getRecordedAt());
        response.setRollbackDetected(rollbackDetected);
        response.setPreviousReading(previousReadingValue);
        response.setWarningMessage(warningMessage);
        
        log.info("Odometer reading saved: readingId={}, rollbackDetected={}", odometerJpa.getReadingId(), rollbackDetected);
        return response;
    }

    /**
     * Complete check-in process.
     * Creates service ticket, updates booking status, creates customer auth.
     * 
     * @param request CompleteCheckInRequest with ticket code and notes
     * @return ServiceTicketResponse with ticket information and warnings
     */
    @Transactional
    public ServiceTicketResponse completeCheckIn(CompleteCheckInRequest request) {
        log.info("Completing check-in for ticket: {}", request.getTicketCode());
        
        // 1. Validate all required data present
        ServiceTicketJpa ticketJpa = serviceTicketRepository.findByTicketCode(request.getTicketCode())
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket"));
        
        // Validate odometer reading exists
        Optional<OdometerHistoryJpa> odometerReading = odometerRepository.findLatestByVehicleId(ticketJpa.getVehicleId());
        if (!odometerReading.isPresent()) {
            throw new CheckInException("Chưa nhập số công tơ mét");
        }
        
        // Validate at least one photo exists
        List<VehicleConditionPhotoJpa> photos = photoRepository.findAll();
        List<VehicleConditionPhotoJpa> ticketPhotos = new ArrayList<>();
        for (VehicleConditionPhotoJpa photo : photos) {
            if (photo.getServiceTicketId().equals(ticketJpa.getServiceTicketId())) { // Use ID comparison
                ticketPhotos.add(photo);
            }
        }
        if (ticketPhotos.isEmpty()) {
            throw new CheckInException("Chưa upload ảnh xe");
        }
        
        // 2. Update ServiceTicket entity
        ticketJpa.setTicketStatus(TicketStatus.CREATED);
        ticketJpa.setOdometerReading(odometerReading.get().getReading());
        ticketJpa.setCheckInNotes(request.getCheckInNotes());
        
        // 3. Set immutable flag
        ticketJpa.setImmutable(true);
        ticketJpa.setUpdatedAt(LocalDateTime.now());
        ticketJpa = serviceTicketRepository.save(ticketJpa);
        
        // 4. Update booking status to DONE
        Booking booking = bookingService.findById(ticketJpa.getBookingId());
        booking.setStatus(BookingStatus.DONE);
        bookingRepository.save(booking);
        
        // 5. Check appointment discrepancy
        List<ServiceTicketResponse.Warning> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        if (!booking.getScheduledDate().equals(today)) {
            ServiceTicketResponse.Warning warning = new ServiceTicketResponse.Warning();
            warning.setCode("APPOINTMENT_DATE_MISMATCH");
            warning.setMessage(String.format("Khách hàng đến sai ngày hẹn. Ngày hẹn: %s, Ngày đến: %s", 
                booking.getScheduledDate(), today));
            warning.setSeverity("WARNING");
            warnings.add(warning);
        }
        
        // Check if time is significantly different (more than 30 minutes)
        LocalTime scheduledTime = booking.getScheduledTime();
        int minutesDiff = Math.abs(now.toSecondOfDay() - scheduledTime.toSecondOfDay()) / 60;
        if (minutesDiff > 30) {
            ServiceTicketResponse.Warning warning = new ServiceTicketResponse.Warning();
            warning.setCode("APPOINTMENT_TIME_MISMATCH");
            warning.setMessage(String.format("Khách hàng đến sai giờ hẹn. Giờ hẹn: %s, Giờ đến: %s", 
                scheduledTime, now));
            warning.setSeverity("INFO");
            warnings.add(warning);
        }
        
        // 6. Create customer auth if needed
        Optional<CustomerAuth> existingAuth = customerAuthRepository.findByCustomerId(booking.getCustomerId());
        if (!existingAuth.isPresent()) {
            CustomerAuth customerAuth = new CustomerAuth();
            customerAuth.setCustomerId(booking.getCustomerId());
            customerAuth.setStatus(CustomerStatus.INACTIVE);
            customerAuth.setFailedAttemptCount(0);
            customerAuth.setOtpAttemptCount(0);
            customerAuth.setCreatedAt(LocalDateTime.now());
            customerAuthRepository.save(customerAuth);
            log.info("Created customer auth for customerId={}", booking.getCustomerId());
            
            // TODO: Send OTP for activation (will be implemented in Phase 2)
        }
        
        // 7. Return ServiceTicketResponse with warnings
        ServiceTicketResponse response = new ServiceTicketResponse();
        response.setServiceTicketId(ticketJpa.getServiceTicketId());
        response.setTicketCode(ticketJpa.getTicketCode());
        response.setBookingId(ticketJpa.getBookingId());
        response.setVehicleId(ticketJpa.getVehicleId());
        response.setTicketStatus(ticketJpa.getTicketStatus());
        response.setOdometerReading(ticketJpa.getOdometerReading());
        response.setCheckInNotes(ticketJpa.getCheckInNotes());
        response.setImmutable(ticketJpa.getImmutable());
        response.setCreatedAt(ticketJpa.getCreatedAt());
        response.setUpdatedAt(ticketJpa.getUpdatedAt());
        
        // Map photos
        List<ServiceTicketResponse.PhotoInfo> photoInfos = new ArrayList<>();
        for (VehicleConditionPhotoJpa photo : ticketPhotos) {
            ServiceTicketResponse.PhotoInfo photoInfo = new ServiceTicketResponse.PhotoInfo();
            photoInfo.setPhotoId(photo.getPhotoId());
            photoInfo.setCategory(photo.getCategory().name());
            photoInfo.setPhotoUrl(photo.getPhotoUrl());
            photoInfo.setDescription(photo.getDescription());
            photoInfos.add(photoInfo);
        }
        response.setPhotos(photoInfos);
        response.setWarnings(warnings);
        
        log.info("Check-in completed successfully: ticketCode={}, warnings={}", ticketJpa.getTicketCode(), warnings.size());
        return response;
    }

    /**
     * Create new service ticket with generated ticket code.
     * Delegates to ServiceTicketService.
     * 
     * @param bookingId Booking ID
     * @param vehicleId Vehicle ID
     * @param customerId Customer ID
     * @return Created ServiceTicket entity
     */
    @Transactional
    public ServiceTicket createServiceTicket(Integer bookingId, Integer vehicleId, Integer customerId) {
        return serviceTicketService.createServiceTicket(bookingId, vehicleId, customerId);
    }

    /**
     * Find service ticket by ticket code.
     * Delegates to ServiceTicketService.
     * 
     * @param ticketCode Ticket code (ST_XXXXXX)
     * @return ServiceTicket entity
     */
    @Transactional(readOnly = true)
    public ServiceTicket findByTicketCode(String ticketCode) {
        return serviceTicketService.findByTicketCode(ticketCode);
    }

    /**
     * Get all vehicles of a customer.
     * Used for vehicle selection dropdown in check-in form.
     * 
     * @param customerId Customer ID
     * @return CustomerVehiclesResponse with list of vehicles
     */
    @Transactional(readOnly = true)
    public CustomerVehiclesResponse getCustomerVehicles(Integer customerId) {
        log.info("Getting vehicles for customer: {}", customerId);
        
        // 1. Validate customer exists
        CustomerProfile customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
        
        // 2. Get all vehicles
        List<Vehicle> vehicles = vehicleRepository.findByCustomer_CustomerId(customerId);
        
        // 3. Map to response
        CustomerVehiclesResponse response = new CustomerVehiclesResponse();
        response.setCustomerId(customerId);
        
        List<CustomerVehiclesResponse.VehicleInfo> vehicleInfos = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            CustomerVehiclesResponse.VehicleInfo info = new CustomerVehiclesResponse.VehicleInfo();
            info.setVehicleId(vehicle.getVehicleId());
            info.setLicensePlate(vehicle.getLicensePlate());
            info.setMake(vehicle.getBrand());
            info.setModel(vehicle.getModel());
            info.setYear(vehicle.getManufactureYear());
            
            // Get last odometer reading
            Optional<OdometerHistoryJpa> lastReading = odometerRepository.findLatestByVehicleId(vehicle.getVehicleId());
            if (lastReading.isPresent()) {
                info.setLastOdometerReading(lastReading.get().getReading());
            }
            
            // Get last service date
            List<ServiceTicketJpa> tickets = serviceTicketRepository.findAll();
            ServiceTicketJpa lastTicket = null;
            for (ServiceTicketJpa ticket : tickets) {
                if (ticket.getVehicleId().equals(vehicle.getVehicleId())) {
                    if (lastTicket == null || ticket.getCreatedAt().isAfter(lastTicket.getCreatedAt())) {
                        lastTicket = ticket;
                    }
                }
            }
            if (lastTicket != null) {
                info.setLastServiceDate(lastTicket.getCreatedAt().toLocalDate());
            }
            
            vehicleInfos.add(info);
        }
        
        response.setVehicles(vehicleInfos);
        
        log.info("Found {} vehicles for customer: {}", vehicleInfos.size(), customerId);
        return response;
    }

    /**
     * Complete check-in in single page form (all-in-one).
     * This method handles:
     * 1. Select/create vehicle
     * 2. Create service ticket
     * 3. Upload all photos
     * 4. Save odometer reading
     * 5. Complete check-in (status → CREATED)
     * 
     * All operations are done in a single transaction.
     * 
     * @param request CompleteCheckInAllRequest with all check-in data
     * @return ServiceTicketResponse with ticket information and warnings
     */
    @Transactional
    public ServiceTicketResponse completeCheckInAll(CompleteCheckInAllRequest request) {
        log.info("Starting single-page check-in for booking: {}", request.getBookingId());
        
        // 1. Select or create vehicle
        Vehicle vehicle;
        boolean isNewVehicle = false;
        
        if (request.getVehicleId() != null) {
            // Use existing vehicle
            vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new CheckInException("Không tìm thấy xe với ID: " + request.getVehicleId()));
            log.info("Using existing vehicle: vehicleId={}", vehicle.getVehicleId());
        } else {
            // Create new vehicle
            if (request.getLicensePlate() == null || request.getLicensePlate().trim().isEmpty()) {
                throw new CheckInException("Biển số xe là bắt buộc khi tạo xe mới");
            }
            if (request.getMake() == null || request.getMake().trim().isEmpty()) {
                throw new CheckInException("Hãng xe là bắt buộc khi tạo xe mới");
            }
            if (request.getModel() == null || request.getModel().trim().isEmpty()) {
                throw new CheckInException("Model xe là bắt buộc khi tạo xe mới");
            }
            if (request.getYear() == null) {
                throw new CheckInException("Năm sản xuất là bắt buộc khi tạo xe mới");
            }
            
            // Validate license plate uniqueness
            Optional<Vehicle> existingVehicle = vehicleRepository.findByLicensePlate(request.getLicensePlate());
            if (existingVehicle.isPresent()) {
                throw new CheckInException("Biển số xe đã tồn tại: " + request.getLicensePlate());
            }
            
            vehicle = new Vehicle();
            vehicle.setLicensePlate(request.getLicensePlate());
            vehicle.setBrand(request.getMake());
            vehicle.setModel(request.getModel());
            vehicle.setManufactureYear(request.getYear());
            
            CustomerProfile customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
            vehicle.setCustomer(customer);
            
            vehicle = vehicleRepository.save(vehicle);
            isNewVehicle = true;
            log.info("Created new vehicle: vehicleId={}, licensePlate={}", vehicle.getVehicleId(), vehicle.getLicensePlate());
        }
        
        // 2. Create Service Ticket (ONLY CREATED HERE - NO DUPLICATES!)
        ServiceTicket serviceTicket = serviceTicketService.createServiceTicket(
            request.getBookingId(),
            vehicle.getVehicleId(),
            request.getCustomerId()
        );
        log.info("Created service ticket: ticketCode={}", serviceTicket.getTicketCode());
        
        ServiceTicketJpa ticketJpa = serviceTicketRepository.findByTicketCode(serviceTicket.getTicketCode())
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket vừa tạo"));
        
        // 3. Upload license plate photo (optional)
        if (request.getLicensePlatePhoto() != null && !request.getLicensePlatePhoto().isEmpty()) {
            try {
                String photoUrl = imageUploadService.uploadImage(
                    request.getLicensePlatePhoto(), 
                    FileUploadConstants.FOLDER_VEHICLE
                );
                ticketJpa.setLicensePlatePhotoUrl(photoUrl);
                log.info("Uploaded license plate photo: {}", photoUrl);
            } catch (IOException e) {
                log.error("Failed to upload license plate photo", e);
                throw new CheckInException("Không thể upload ảnh biển số: " + e.getMessage());
            }
        }
        
        // 4. Upload all vehicle condition photos
        int photoCount = 0;
        
        // Validate staffId field
        if (request.getStaffId() == null) {
            throw new CheckInException("Thiếu thông tin nhân viên thực hiện check-in (staffId)");
        }
        
        try {
            // Upload front photo
            if (request.getPhotoFront() != null && !request.getPhotoFront().isEmpty()) {
                String photoUrl = imageUploadService.uploadImage(request.getPhotoFront(), FileUploadConstants.FOLDER_VEHICLE);
                VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
                photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
                photoJpa.setCategory(PhotoCategory.FRONT);
                photoJpa.setPhotoUrl(photoUrl);
                photoJpa.setDescription(request.getPhotoFrontDescription());
                photoJpa.setUploadedAt(LocalDateTime.now());
                photoJpa.setUploadedBy(request.getStaffId());
                photoRepository.save(photoJpa);
                photoCount++;
                log.info("Uploaded photo: category=FRONT, url={}", photoUrl);
            }
            
            // Upload rear photo
            if (request.getPhotoRear() != null && !request.getPhotoRear().isEmpty()) {
                String photoUrl = imageUploadService.uploadImage(request.getPhotoRear(), FileUploadConstants.FOLDER_VEHICLE);
                VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
                photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
                photoJpa.setCategory(PhotoCategory.BACK);
                photoJpa.setPhotoUrl(photoUrl);
                photoJpa.setDescription(request.getPhotoRearDescription());
                photoJpa.setUploadedAt(LocalDateTime.now());
                photoJpa.setUploadedBy(request.getStaffId());
                photoRepository.save(photoJpa);
                photoCount++;
                log.info("Uploaded photo: category=BACK, url={}", photoUrl);
            }
            
            // Upload left side photo
            if (request.getPhotoLeftSide() != null && !request.getPhotoLeftSide().isEmpty()) {
                String photoUrl = imageUploadService.uploadImage(request.getPhotoLeftSide(), FileUploadConstants.FOLDER_VEHICLE);
                VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
                photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
                photoJpa.setCategory(PhotoCategory.LEFT);
                photoJpa.setPhotoUrl(photoUrl);
                photoJpa.setDescription(request.getPhotoLeftSideDescription());
                photoJpa.setUploadedAt(LocalDateTime.now());
                photoJpa.setUploadedBy(request.getStaffId());
                photoRepository.save(photoJpa);
                photoCount++;
                log.info("Uploaded photo: category=LEFT, url={}", photoUrl);
            }
            
            // Upload right side photo
            if (request.getPhotoRightSide() != null && !request.getPhotoRightSide().isEmpty()) {
                String photoUrl = imageUploadService.uploadImage(request.getPhotoRightSide(), FileUploadConstants.FOLDER_VEHICLE);
                VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
                photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
                photoJpa.setCategory(PhotoCategory.RIGHT);
                photoJpa.setPhotoUrl(photoUrl);
                photoJpa.setDescription(request.getPhotoRightSideDescription());
                photoJpa.setUploadedAt(LocalDateTime.now());
                photoJpa.setUploadedBy(request.getStaffId());
                photoRepository.save(photoJpa);
                photoCount++;
                log.info("Uploaded photo: category=RIGHT, url={}", photoUrl);
            }
            
            // Upload interior photo
            if (request.getPhotoInterior() != null && !request.getPhotoInterior().isEmpty()) {
                String photoUrl = imageUploadService.uploadImage(request.getPhotoInterior(), FileUploadConstants.FOLDER_VEHICLE);
                VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
                photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
                photoJpa.setCategory(PhotoCategory.OVERALL);
                photoJpa.setPhotoUrl(photoUrl);
                photoJpa.setDescription(request.getPhotoInteriorDescription());
                photoJpa.setUploadedAt(LocalDateTime.now());
                photoJpa.setUploadedBy(request.getStaffId());
                photoRepository.save(photoJpa);
                photoCount++;
                log.info("Uploaded photo: category=OVERALL, url={}", photoUrl);
            }
            
            // Upload damage photo
            if (request.getPhotoDamage() != null && !request.getPhotoDamage().isEmpty()) {
                String photoUrl = imageUploadService.uploadImage(request.getPhotoDamage(), FileUploadConstants.FOLDER_VEHICLE);
                VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
                photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
                photoJpa.setCategory(PhotoCategory.DAMAGE);
                photoJpa.setPhotoUrl(photoUrl);
                photoJpa.setDescription(request.getPhotoDamageDescription());
                photoJpa.setUploadedAt(LocalDateTime.now());
                photoJpa.setUploadedBy(request.getStaffId());
                photoRepository.save(photoJpa);
                photoCount++;
                log.info("Uploaded photo: category=DAMAGE, url={}", photoUrl);
            }
        } catch (IOException e) {
            log.error("Failed to upload vehicle condition photo", e);
            throw new CheckInException("Không thể upload ảnh xe: " + e.getMessage());
        }
        
        // Validate at least 1 photo
        if (photoCount == 0) {
            throw new CheckInException("Phải upload ít nhất 1 ảnh tình trạng xe");
        }
        
        log.info("Uploaded {} vehicle condition photos", photoCount);
        
        // 5. Save odometer reading
        if (request.getOdometerReading() == null) {
            throw new CheckInException("Số công tơ mét là bắt buộc");
        }
        
        // Check for odometer rollback
        Optional<OdometerHistoryJpa> previousReading = odometerRepository.findLatestByVehicleId(vehicle.getVehicleId());
        boolean rollbackDetected = false;
        Integer previousReadingValue = null;
        
        if (previousReading.isPresent()) {
            previousReadingValue = previousReading.get().getReading();
            if (request.getOdometerReading() < previousReadingValue) {
                rollbackDetected = true;
                log.warn("Odometer rollback detected: vehicleId={}, previous={}, current={}", 
                    vehicle.getVehicleId(), previousReadingValue, request.getOdometerReading());
            }
        }
        
        OdometerHistoryJpa odometerJpa = new OdometerHistoryJpa();
        odometerJpa.setVehicleId(vehicle.getVehicleId());
        odometerJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
        odometerJpa.setReading(request.getOdometerReading());
        odometerJpa.setRecordedAt(LocalDateTime.now());
        odometerJpa.setRecordedBy(request.getStaffId());
        odometerJpa.setRollbackDetected(rollbackDetected);
        odometerJpa.setPreviousReading(previousReadingValue);
        odometerRepository.save(odometerJpa);
        
        log.info("Saved odometer reading: {} km, rollbackDetected={}", request.getOdometerReading(), rollbackDetected);
        
        // 6. Update Service Ticket to CREATED status
        ticketJpa.setTicketStatus(TicketStatus.CREATED);
        ticketJpa.setOdometerReading(request.getOdometerReading());
        ticketJpa.setCheckInNotes(request.getCheckInNotes());
        ticketJpa.setImmutable(true);
        ticketJpa.setUpdatedAt(LocalDateTime.now());
        ticketJpa = serviceTicketRepository.save(ticketJpa);
        
        log.info("Updated ticket status to CREATED: {}", ticketJpa.getTicketCode());
        
        // 7. Update booking status to DONE
        Booking booking = bookingService.findById(request.getBookingId());
        booking.setStatus(BookingStatus.DONE);
        bookingRepository.save(booking);
        
        log.info("Updated booking status to DONE: bookingId={}", request.getBookingId());
        
        // 8. Check appointment discrepancy and create warnings
        List<ServiceTicketResponse.Warning> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        if (rollbackDetected) {
            ServiceTicketResponse.Warning warning = new ServiceTicketResponse.Warning();
            warning.setCode("ODOMETER_ROLLBACK");
            warning.setMessage(String.format("CẢNH BÁO: Số công tơ mét hiện tại (%d km) nhỏ hơn lần trước (%d km). Có thể đã bị lùi công tơ mét.", 
                request.getOdometerReading(), previousReadingValue));
            warning.setSeverity("WARNING");
            warnings.add(warning);
        }
        
        if (!booking.getScheduledDate().equals(today)) {
            ServiceTicketResponse.Warning warning = new ServiceTicketResponse.Warning();
            warning.setCode("APPOINTMENT_DATE_MISMATCH");
            warning.setMessage(String.format("Khách hàng đến sai ngày hẹn. Ngày hẹn: %s, Ngày đến: %s", 
                booking.getScheduledDate(), today));
            warning.setSeverity("WARNING");
            warnings.add(warning);
        }
        
        LocalTime scheduledTime = booking.getScheduledTime();
        int minutesDiff = Math.abs(now.toSecondOfDay() - scheduledTime.toSecondOfDay()) / 60;
        if (minutesDiff > 30) {
            ServiceTicketResponse.Warning warning = new ServiceTicketResponse.Warning();
            warning.setCode("APPOINTMENT_TIME_MISMATCH");
            warning.setMessage(String.format("Khách hàng đến sai giờ hẹn. Giờ hẹn: %s, Giờ đến: %s", 
                scheduledTime, now));
            warning.setSeverity("INFO");
            warnings.add(warning);
        }
        
        // 9. Create customer auth if needed
        Optional<CustomerAuth> existingAuth = customerAuthRepository.findByCustomerId(request.getCustomerId());
        if (!existingAuth.isPresent()) {
            CustomerAuth customerAuth = new CustomerAuth();
            customerAuth.setCustomerId(request.getCustomerId());
            customerAuth.setStatus(CustomerStatus.INACTIVE);
            customerAuth.setFailedAttemptCount(0);
            customerAuth.setOtpAttemptCount(0);
            customerAuth.setCreatedAt(LocalDateTime.now());
            customerAuthRepository.save(customerAuth);
            log.info("Created customer auth for customerId={}", request.getCustomerId());
        }
        
        // 10. Build response using mapper (Clean Architecture pattern)
        // Step 1: JPA → Domain (Infrastructure Mapper)
        ServiceTicket serviceTicketDomain = serviceTicketMapper.toDomain(ticketJpa);
        
        // Step 2: Domain → DTO (API Mapper)
        ServiceTicketResponse response = serviceTicketDtoMapper.toResponse(serviceTicketDomain);
        
        // Step 3: Map photos (business logic - photos not in Domain entity)
        List<VehicleConditionPhotoJpa> allPhotos = photoRepository.findAll();
        List<ServiceTicketResponse.PhotoInfo> photoInfos = new ArrayList<>();
        for (VehicleConditionPhotoJpa photo : allPhotos) {
            if (photo.getServiceTicketId().equals(ticketJpa.getServiceTicketId())) {
                ServiceTicketResponse.PhotoInfo photoInfo = new ServiceTicketResponse.PhotoInfo();
                photoInfo.setPhotoId(photo.getPhotoId());
                photoInfo.setCategory(photo.getCategory().name());
                photoInfo.setPhotoUrl(photo.getPhotoUrl());
                photoInfo.setDescription(photo.getDescription());
                photoInfos.add(photoInfo);
            }
        }
        response.setPhotos(photoInfos);
        
        // Step 4: Set warnings (business logic)
        response.setWarnings(warnings);
        
        log.info("Single-page check-in completed successfully: ticketCode={}, photoCount={}, warnings={}", 
            ticketJpa.getTicketCode(), photoCount, warnings.size());
        
        return response;
    }

}
