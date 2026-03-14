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
import com.g42.platform.gms.service_ticket_management.api.mapper.BookingLookupMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.PhotoResponseMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.VehicleMapper;
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
 * Service layer cho quy trình Check-in.
 * Xử lý business logic cho việc check-in xe (bước đầu tiên của service ticket lifecycle).
 * 
 * Chức năng chính:
 * - Lookup booking theo mã booking
 * - Tạo xe mới cho customer
 * - Lưu/chọn xe cho check-in
 * - Upload ảnh biển số và ảnh tình trạng xe
 * - Lưu số công tơ mét với phát hiện rollback
 * - Hoàn tất check-in (chuyển status từ DRAFT → CREATED)
 * 
 * Business Rules:
 * - Service Ticket được tạo tự động khi chọn xe (lazy creation)
 * - Service Ticket reuse booking_code làm ticket_code (đảm bảo đồng bộ mã)
 * - Phải có ít nhất 1 ảnh xe và số công tơ mét trước khi complete check-in
 * - Phát hiện odometer rollback và cảnh báo
 * - Phát hiện khách đến sai ngày/giờ hẹn và cảnh báo
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
    private final BookingLookupMapper bookingLookupMapper;
    private final VehicleMapper vehicleMapper;
    private final PhotoResponseMapper photoResponseMapper;

    /**
     * Lookup booking theo mã booking.
     * Trả về thông tin booking với thông tin customer và danh sách dịch vụ.
     * 
     * @param request BookingLookupRequest chứa booking code
     * @return BookingLookupResponse với thông tin booking và customer
     */
    @Transactional(readOnly = true)
    public BookingLookupResponse lookupBooking(BookingLookupRequest request) {
        log.info("Looking up booking: {}", request.getBookingCode());
        
        // Query booking sử dụng BookingService
        Booking booking = bookingService.findByCode(request.getBookingCode());
        
        // Lấy thông tin customer
        CustomerProfile customer = customerRepository.findById(booking.getCustomerId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy thông tin khách hàng"));
        
        // Lấy thông tin các dịch vụ từ booking
        List<com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity> catalogItems = new ArrayList<>();
        if (booking.getCatalogItemIds() != null && !booking.getCatalogItemIds().isEmpty()) {
            for (Integer catalogItemId : booking.getCatalogItemIds()) {
                Optional<com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity> catalogItem = 
                    catalogRepository.findById(catalogItemId);
                if (catalogItem.isPresent()) {
                    catalogItems.add(catalogItem.get());
                }
            }
        }
        
        // Map sang BookingLookupResponse sử dụng mapper
        BookingLookupResponse response = bookingLookupMapper.toResponse(booking, customer);
        response.setServices(bookingLookupMapper.toServiceInfoList(catalogItems));
        
        log.info("Booking lookup successful: bookingId={}, customerId={}", booking.getBookingId(), customer.getCustomerId());
        return response;
    }

    /**
     * Tạo xe mới cho customer.
     * Sử dụng khi customer chưa có xe trong hệ thống.
     * 
     * @param request CreateVehicleRequest với thông tin xe
     * @return CreateVehicleResponse với thông tin xe đã tạo
     */
    @Transactional
    public CreateVehicleResponse createVehicle(CreateVehicleRequest request) {
        log.info("Creating new vehicle for customer: {}, licensePlate: {}", 
            request.getCustomerId(), request.getLicensePlate());
        
        // Validate customer exists
        CustomerProfile customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
        
        // Validate license plate uniqueness
        Optional<Vehicle> existingVehicle = vehicleRepository.findByLicensePlate(request.getLicensePlate());
        if (existingVehicle.isPresent()) {
            throw new CheckInException("Biển số xe đã tồn tại: " + request.getLicensePlate());
        }
        
        // Create new vehicle
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setBrand(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setManufactureYear(request.getYear());
        vehicle.setCustomer(customer);
        
        vehicle = vehicleRepository.save(vehicle);
        log.info("Created new vehicle: vehicleId={}, licensePlate={}", 
            vehicle.getVehicleId(), vehicle.getLicensePlate());
        
        // Map to CreateVehicleResponse sử dụng mapper
        return vehicleMapper.toCreateResponse(vehicle);
    }

    /**
     * Lưu hoặc chọn xe cho check-in.
     * Nếu có vehicleId: validate và trả về xe có sẵn.
     * Nếu vehicleId null: tạo xe mới.
     * Tự động tạo Service Ticket nếu chưa tồn tại cho booking này (lazy creation).
     * 
     * @param request VehicleRequest với thông tin xe
     * @return VehicleResponse với thông tin xe và ticket code
     */
    @Transactional
    public VehicleResponse saveVehicle(VehicleRequest request) {
        log.info("Saving vehicle - vehicleId: {}, licensePlate: {}", request.getVehicleId(), request.getLicensePlate());
        
        Vehicle vehicle;
        boolean isNewVehicle = false;
        
        // Nếu có vehicleId, validate và trả về xe có sẵn
        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new CheckInException("Không tìm thấy xe với ID: " + request.getVehicleId()));
            log.info("Using existing vehicle: vehicleId={}", vehicle.getVehicleId());
        } else {
            // Nếu vehicleId null, tạo xe mới
            // Validate các trường bắt buộc khi tạo xe mới
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
            
            // Validate biển số xe không trùng
            Optional<Vehicle> existingVehicle = vehicleRepository.findByLicensePlate(request.getLicensePlate());
            if (existingVehicle.isPresent()) {
                throw new CheckInException("Biển số xe đã tồn tại: " + request.getLicensePlate());
            }
            
            vehicle = new Vehicle();
            vehicle.setLicensePlate(request.getLicensePlate());
            vehicle.setBrand(request.getMake());
            vehicle.setModel(request.getModel());
            vehicle.setManufactureYear(request.getYear());
            
            // Liên kết xe với customer
            CustomerProfile customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
            vehicle.setCustomer(customer);
            
            vehicle = vehicleRepository.save(vehicle);
            isNewVehicle = true;
            log.info("Created new vehicle: vehicleId={}, licensePlate={}", vehicle.getVehicleId(), vehicle.getLicensePlate());
        }
        
        // Kiểm tra Service Ticket đã tồn tại cho booking này chưa
        ServiceTicket serviceTicket;
        Optional<ServiceTicketJpa> existingTicket = serviceTicketRepository.findByBookingId(request.getBookingId());
        
        if (existingTicket.isPresent()) {
            // Ticket đã tồn tại - trả về ticket có sẵn
            serviceTicket = serviceTicketMapper.toDomain(existingTicket.get());
            log.info("Service ticket already exists for booking: {}, ticketCode: {}", 
                request.getBookingId(), serviceTicket.getTicketCode());
            
            // Cập nhật vehicle_id nếu thay đổi
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
            // Tạo Service Ticket mới (lazy creation)
            Integer staffId = request.getStaffId() != null ? request.getStaffId() : 0;
            serviceTicket = serviceTicketService.createServiceTicket(
                request.getBookingId(),
                vehicle.getVehicleId(),
                request.getCustomerId(),
                staffId
            );
            log.info("Created new service ticket: ticketCode={}", serviceTicket.getTicketCode());
        }
        
        // Map sang VehicleResponse sử dụng mapper
        VehicleResponse response = vehicleMapper.toVehicleResponse(vehicle);
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
        
        // Upload to Cloudinary
        String photoUrl;
        try {
            photoUrl = imageUploadService.uploadImage(file, FileUploadConstants.FOLDER_VEHICLE);
        } catch (IOException e) {
            log.error("Failed to upload condition photo", e);
            throw new CheckInException("Không thể upload ảnh: " + e.getMessage());
        }
        
        // Find service ticket
        ServiceTicket ticket = serviceTicketService.findByTicketCode(request.getTicketCode());
        ServiceTicketJpa ticketJpa = serviceTicketRepository.findByTicketCode(request.getTicketCode())
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket"));
        
        // Create VehicleConditionPhotoJpa entity
        VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
        photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
        photoJpa.setCategory(request.getCategory());
        photoJpa.setPhotoUrl(photoUrl);
        photoJpa.setDescription(request.getDescription());
        photoJpa.setUploadedAt(LocalDateTime.now());
        photoJpa.setUploadedBy(request.getUploadedBy());
        
        // Save to database
        photoJpa = photoRepository.save(photoJpa);
        
        // Map to PhotoUploadResponse sử dụng mapper
        PhotoUploadResponse response = photoResponseMapper.toUploadResponse(photoJpa);
        
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
        ticketJpa.setCheckInNotes(request.getCheckInNotes());
        ticketJpa.setReceivedAt(LocalDateTime.now()); // Set thời điểm khách hàng đến garage
        
        // 3. Không set immutable flag - chỉ dùng status để kiểm soát quyền edit
        // immutable sẽ được set khi ticket chuyển sang COMPLETED
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
        // TODO: TEMPORARILY DISABLED - Will create account when confirming booking instead of check-in
        // Optional<CustomerAuth> existingAuth = customerAuthRepository.findByCustomerId(booking.getCustomerId());
        // if (!existingAuth.isPresent()) {
        //     CustomerAuth customerAuth = new CustomerAuth();
        //     customerAuth.setCustomerId(booking.getCustomerId());
        //     customerAuth.setStatus(CustomerStatus.INACTIVE);
        //     customerAuth.setFailedAttemptCount(0);
        //     customerAuth.setOtpAttemptCount(0);
        //     customerAuth.setCreatedAt(LocalDateTime.now());
        //     customerAuthRepository.save(customerAuth);
        //     log.info("Created customer auth for customerId={}", booking.getCustomerId());
        //     
        //     // TODO: Send OTP for activation (will be implemented in Phase 2)
        // }
        
        // 7. Return ServiceTicketResponse with warnings
        ServiceTicketResponse response = new ServiceTicketResponse();
        response.setServiceTicketId(ticketJpa.getServiceTicketId());
        response.setTicketCode(ticketJpa.getTicketCode());
        response.setBookingId(ticketJpa.getBookingId());
        response.setVehicleId(ticketJpa.getVehicleId());
        response.setTicketStatus(ticketJpa.getTicketStatus());
        response.setCheckInNotes(ticketJpa.getCheckInNotes());
        response.setImmutable(ticketJpa.getImmutable());
        response.setCreatedAt(ticketJpa.getCreatedAt());
        response.setUpdatedAt(ticketJpa.getUpdatedAt());
        
        // Map photos sử dụng mapper
        List<ServiceTicketResponse.PhotoInfo> photoInfos = photoResponseMapper.toPhotoInfoList(ticketPhotos);
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
     * @param createdBy Staff ID who creates the ticket
     * @return Created ServiceTicket entity
     */
    @Transactional
    public ServiceTicket createServiceTicket(Integer bookingId, Integer vehicleId, Integer customerId, Integer createdBy) {
        return serviceTicketService.createServiceTicket(bookingId, vehicleId, customerId, createdBy);
    }

    /**
     * Find service ticket by ticket code.
     * Delegates to ServiceTicketService.
     * 
     * @param ticketCode Ticket code (MST_XXXXXX)
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
        
        // Validate customer exists
        CustomerProfile customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));
        
        // Get all vehicles
        List<Vehicle> vehicles = vehicleRepository.findByCustomer_CustomerId(customerId);
        
        // Map to VehicleInfo sử dụng mapper
        List<CustomerVehiclesResponse.VehicleInfo> vehicleInfos = vehicleMapper.toVehicleInfoList(vehicles);
        
        // Set business logic fields (odometer và last service date)
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            CustomerVehiclesResponse.VehicleInfo info = vehicleInfos.get(i);
            
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
        }
        
        // Build response
        CustomerVehiclesResponse response = new CustomerVehiclesResponse();
        response.setCustomerId(customerId);
        response.setVehicles(vehicleInfos);
        
        log.info("Found {} vehicles for customer: {}", vehicleInfos.size(), customerId);
        return response;
    }

    /**
     * Complete check-in in single page form (all-in-one).
     * This method handles:
     * 1. Validate and get vehicle (must exist - created via /vehicles/create endpoint)
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
        
        // Validate staffId field
        if (request.getStaffId() == null) {
            throw new CheckInException("Thiếu thông tin nhân viên thực hiện check-in (staffId)");
        }
        
        // Validate and get vehicle (must exist)
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy xe với ID: " + request.getVehicleId()));
        log.info("Using vehicle: vehicleId={}, licensePlate={}", 
            vehicle.getVehicleId(), vehicle.getLicensePlate());
        
        // Create Service Ticket
        ServiceTicket serviceTicket = serviceTicketService.createServiceTicket(
            request.getBookingId(),
            vehicle.getVehicleId(),
            request.getCustomerId(),
            request.getStaffId()
        );
        log.info("Created service ticket: ticketCode={}", serviceTicket.getTicketCode());
        
        ServiceTicketJpa ticketJpa = serviceTicketRepository.findByTicketCode(serviceTicket.getTicketCode())
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket vừa tạo"));
        
        // === 3. Upload license plate photo (optional) - save to vehicle_condition_photo table ===
        if (request.getLicensePlatePhoto() != null && !request.getLicensePlatePhoto().isEmpty()) {
            try {
                String photoUrl = imageUploadService.uploadImage(
                    request.getLicensePlatePhoto(), 
                    FileUploadConstants.FOLDER_VEHICLE
                );
                
                // Save as LICENSE_PLATE category in vehicle_condition_photo
                VehicleConditionPhotoJpa photoJpa = new VehicleConditionPhotoJpa();
                photoJpa.setServiceTicketId(ticketJpa.getServiceTicketId());
                photoJpa.setCategory(PhotoCategory.LICENSE_PLATE);
                photoJpa.setPhotoUrl(photoUrl);
                photoJpa.setDescription("Ảnh biển số xe");
                photoJpa.setUploadedAt(LocalDateTime.now());
                photoJpa.setUploadedBy(request.getStaffId());
                photoRepository.save(photoJpa);
                
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
        ticketJpa.setCheckInNotes(request.getCheckInNotes());
        ticketJpa.setReceivedAt(LocalDateTime.now()); // Set thời điểm khách hàng đến garage
        
        // Không set immutable flag - chỉ dùng status để kiểm soát quyền edit
        // immutable sẽ được set khi ticket chuyển sang COMPLETED
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
        // TODO: TEMPORARILY DISABLED - Will create account when confirming booking instead of check-in
        // Optional<CustomerAuth> existingAuth = customerAuthRepository.findByCustomerId(request.getCustomerId());
        // if (!existingAuth.isPresent()) {
        //     CustomerAuth customerAuth = new CustomerAuth();
        //     customerAuth.setCustomerId(request.getCustomerId());
        //     customerAuth.setStatus(CustomerStatus.INACTIVE);
        //     customerAuth.setFailedAttemptCount(0);
        //     customerAuth.setOtpAttemptCount(0);
        //     customerAuth.setCreatedAt(LocalDateTime.now());
        //     customerAuthRepository.save(customerAuth);
        //     log.info("Created customer auth for customerId={}", request.getCustomerId());
        // }
        
        // 10. Build response using mapper (Clean Architecture pattern)
        // Step 1: JPA → Domain (Infrastructure Mapper)
        ServiceTicket serviceTicketDomain = serviceTicketMapper.toDomain(ticketJpa);
        
        // Step 2: Domain → DTO (API Mapper)
        ServiceTicketResponse response = serviceTicketDtoMapper.toResponse(serviceTicketDomain);
        
        // Map photos sử dụng mapper (business logic - photos not in Domain entity)
        List<VehicleConditionPhotoJpa> allPhotos = photoRepository.findAll();
        List<VehicleConditionPhotoJpa> ticketPhotos = new ArrayList<>();
        for (VehicleConditionPhotoJpa photo : allPhotos) {
            if (photo.getServiceTicketId().equals(ticketJpa.getServiceTicketId())) {
                ticketPhotos.add(photo);
            }
        }
        List<ServiceTicketResponse.PhotoInfo> photoInfos = photoResponseMapper.toPhotoInfoList(ticketPhotos);
        response.setPhotos(photoInfos);
        
        // Step 4: Set warnings (business logic)
        response.setWarnings(warnings);
        
        log.info("Single-page check-in completed successfully: ticketCode={}, photoCount={}, warnings={}", 
            ticketJpa.getTicketCode(), photoCount, warnings.size());
        
        return response;
    }

}
