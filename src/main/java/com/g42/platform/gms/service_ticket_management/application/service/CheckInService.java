package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
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
import com.g42.platform.gms.service_ticket_management.domain.repository.OdometerReadingRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.VehicleConditionPhotoRepo;
import com.g42.platform.gms.service_ticket_management.api.mapper.BookingLookupMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.PhotoResponseMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.VehicleMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.ServiceTicketDtoMapper;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInService {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final ServiceTicketService serviceTicketService;
    private final ServiceTicketRepo serviceTicketRepo;
    private final ServiceTicketCodeGenerator ticketCodeGenerator;
    private final VehicleRepository vehicleRepository;
    private final CustomerProfileRepository customerRepository;
    private final CustomerAuthRepository customerAuthRepository;
    private final VehicleConditionPhotoRepo photoRepo;
    private final OdometerReadingRepo odometerRepo;
    private final CatalogItemRepository catalogRepository;
    private final ImageUploadService imageUploadService;
    private final ServiceTicketDtoMapper serviceTicketDtoMapper;
    private final BookingLookupMapper bookingLookupMapper;
    private final VehicleMapper vehicleMapper;
    private final PhotoResponseMapper photoResponseMapper;
    private final SafetyInspectionService safetyInspectionService;

    @Transactional(readOnly = true)
    public BookingLookupResponse lookupBooking(BookingLookupRequest request) {
        log.info("Looking up booking: {}", request.getBookingCode());

        Booking booking = bookingService.findByCode(request.getBookingCode());

        CustomerProfile customer = customerRepository.findById(booking.getCustomerId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy thông tin khách hàng"));

        List<com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity> catalogItems = new ArrayList<>();
        if (booking.getCatalogItemIds() != null && !booking.getCatalogItemIds().isEmpty()) {
            for (Integer catalogItemId : booking.getCatalogItemIds()) {
                catalogRepository.findById(catalogItemId).ifPresent(catalogItems::add);
            }
        }

        BookingLookupResponse response = bookingLookupMapper.toResponse(booking, customer);
        response.setServices(bookingLookupMapper.toServiceInfoList(catalogItems));

        log.info("Booking lookup successful: bookingId={}, customerId={}", booking.getBookingId(), customer.getCustomerId());
        return response;
    }

    @Transactional
    public CreateVehicleResponse createVehicle(CreateVehicleRequest request) {
        log.info("Creating new vehicle for customer: {}, licensePlate: {}",
            request.getCustomerId(), request.getLicensePlate());

        CustomerProfile customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));

        Optional<Vehicle> existingVehicle = vehicleRepository.findByLicensePlate(request.getLicensePlate());
        if (existingVehicle.isPresent()) {
            throw new CheckInException("Biển số xe đã tồn tại: " + request.getLicensePlate());
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setBrand(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setManufactureYear(request.getYear());
        vehicle.setCustomer(customer);

        vehicle = vehicleRepository.save(vehicle);
        log.info("Created new vehicle: vehicleId={}, licensePlate={}", vehicle.getVehicleId(), vehicle.getLicensePlate());

        return vehicleMapper.toCreateResponse(vehicle);
    }

    @Transactional
    public VehicleResponse saveVehicle(VehicleRequest request) {
        log.info("Saving vehicle - vehicleId: {}, licensePlate: {}", request.getVehicleId(), request.getLicensePlate());

        Vehicle vehicle;
        boolean isNewVehicle = false;

        if (request.getVehicleId() != null) {
            vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new CheckInException("Không tìm thấy xe với ID: " + request.getVehicleId()));
        } else {
            if (request.getLicensePlate() == null || request.getLicensePlate().trim().isEmpty())
                throw new CheckInException("Biển số xe là bắt buộc khi tạo xe mới");
            if (request.getMake() == null || request.getMake().trim().isEmpty())
                throw new CheckInException("Hãng xe là bắt buộc khi tạo xe mới");
            if (request.getModel() == null || request.getModel().trim().isEmpty())
                throw new CheckInException("Model xe là bắt buộc khi tạo xe mới");
            if (request.getYear() == null)
                throw new CheckInException("Năm sản xuất là bắt buộc khi tạo xe mới");

            if (vehicleRepository.findByLicensePlate(request.getLicensePlate()).isPresent()) {
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

        ServiceTicket serviceTicket;
        Optional<ServiceTicket> existingTicket = serviceTicketRepo.findByBookingId(request.getBookingId());

        if (existingTicket.isPresent()) {
            serviceTicket = existingTicket.get();
            log.info("Service ticket already exists for booking: {}, ticketCode: {}",
                request.getBookingId(), serviceTicket.getTicketCode());

            if (!serviceTicket.getVehicleId().equals(vehicle.getVehicleId())) {
                serviceTicket.setVehicleId(vehicle.getVehicleId());
                serviceTicketRepo.save(serviceTicket);
            }
        } else {
            Integer staffId = request.getStaffId() != null ? request.getStaffId() : 0;
            serviceTicket = serviceTicketService.createServiceTicket(
                request.getBookingId(), vehicle.getVehicleId(), request.getCustomerId(), staffId);
            log.info("Created new service ticket: ticketCode={}", serviceTicket.getTicketCode());
        }

        VehicleResponse response = vehicleMapper.toVehicleResponse(vehicle);
        response.setIsNewVehicle(isNewVehicle);
        response.setTicketCode(serviceTicket.getTicketCode());

        return response;
    }

    @Transactional
    public PhotoUploadResponse uploadLicensePlatePhoto(MultipartFile file, Integer vehicleId) {
        log.info("Uploading license plate photo for vehicle: {}", vehicleId);

        String photoUrl;
        try {
            photoUrl = imageUploadService.uploadImage(file, FileUploadConstants.FOLDER_VEHICLE);
        } catch (IOException e) {
            log.error("Failed to upload license plate photo", e);
            throw new CheckInException("Không thể upload ảnh biển số: " + e.getMessage());
        }

        PhotoUploadResponse response = new PhotoUploadResponse();
        response.setPhotoUrl(photoUrl);
        response.setCategory(PhotoCategory.ODOMETER);
        response.setUploadedAt(LocalDateTime.now());
        response.setMessage("Upload ảnh biển số thành công");

        log.info("License plate photo uploaded successfully: {}", photoUrl);
        return response;
    }

    @Transactional
    public PhotoUploadResponse uploadConditionPhoto(MultipartFile file, PhotoUploadRequest request) {
        log.info("Uploading condition photo: category={}, ticketCode={}",
            request.getCategory(), request.getTicketCode());

        String photoUrl;
        try {
            photoUrl = imageUploadService.uploadImage(file, FileUploadConstants.FOLDER_VEHICLE);
        } catch (IOException e) {
            log.error("Failed to upload condition photo", e);
            throw new CheckInException("Không thể upload ảnh: " + e.getMessage());
        }

        ServiceTicket ticket = serviceTicketService.findByTicketCode(request.getTicketCode());

        VehicleConditionPhoto photo = new VehicleConditionPhoto();
        photo.setServiceTicketId(ticket.getServiceTicketId());
        photo.setCategory(request.getCategory());
        photo.setPhotoUrl(photoUrl);
        photo.setDescription(request.getDescription());
        photo.setUploadedAt(LocalDateTime.now());
        photo.setUploadedBy(request.getUploadedBy());

        VehicleConditionPhoto saved = photoRepo.save(photo);

        PhotoUploadResponse response = photoResponseMapper.toUploadResponse(saved);

        log.info("Condition photo uploaded successfully: photoId={}, category={}", saved.getPhotoId(), saved.getCategory());
        return response;
    }

    @Transactional
    public OdometerResponse saveOdometer(OdometerRequest request) {
        log.info("Saving odometer reading: {} km for vehicle: {}", request.getReading(), request.getVehicleId());

        Optional<OdometerReading> previousReading = odometerRepo.findLatestByVehicleId(request.getVehicleId());

        boolean rollbackDetected = false;
        Integer previousReadingValue = null;
        String warningMessage = null;

        if (previousReading.isPresent()) {
            previousReadingValue = previousReading.get().getReading();
            if (request.getReading() < previousReadingValue) {
                rollbackDetected = true;
                warningMessage = String.format(
                    "CẢNH BÁO: Số công tơ mét hiện tại (%d km) nhỏ hơn lần trước (%d km). Có thể đã bị lùi công tơ mét.",
                    request.getReading(), previousReadingValue);
                log.warn("Odometer rollback detected: vehicleId={}, previous={}, current={}",
                    request.getVehicleId(), previousReadingValue, request.getReading());
            }
        }

        ServiceTicket ticketForOdometer = serviceTicketService.findByTicketCode(request.getTicketCode());

        OdometerReading reading = new OdometerReading();
        reading.setVehicleId(request.getVehicleId());
        reading.setServiceTicketId(ticketForOdometer.getServiceTicketId());
        reading.setReading(request.getReading());
        reading.setRecordedAt(LocalDateTime.now());

        OdometerReading saved = odometerRepo.save(reading);

        OdometerResponse response = new OdometerResponse();
        response.setReadingId(saved.getReadingId());
        response.setVehicleId(saved.getVehicleId());
        response.setReading(saved.getReading());
        response.setRecordedAt(saved.getRecordedAt());
        response.setRollbackDetected(rollbackDetected);
        response.setPreviousReading(previousReadingValue);
        response.setWarningMessage(warningMessage);

        log.info("Odometer reading saved: readingId={}, rollbackDetected={}", saved.getReadingId(), rollbackDetected);
        return response;
    }

    @Transactional
    public ServiceTicketResponse completeCheckIn(CompleteCheckInRequest request) {
        log.info("Completing check-in for ticket: {}", request.getTicketCode());

        ServiceTicket ticketDomain = serviceTicketService.findByTicketCode(request.getTicketCode());

        // Validate at least one photo exists
        if (!photoRepo.existsByServiceTicketId(ticketDomain.getServiceTicketId())) {
            throw new CheckInException("Chưa upload ảnh xe");
        }

        ticketDomain.setTicketStatus(TicketStatus.DRAFT);
        ticketDomain.setCheckInNotes(request.getCheckInNotes());
        ticketDomain.setReceivedAt(LocalDateTime.now());
        ticketDomain.setUpdatedAt(LocalDateTime.now());
        ServiceTicket savedTicket = serviceTicketRepo.save(ticketDomain);

        Booking booking = bookingService.findById(savedTicket.getBookingId());
        booking.setStatus(BookingStatus.DONE);
        bookingRepository.save(booking);

        if (Boolean.TRUE.equals(request.getSafetyInspection())) {
            Integer staffId = request.getStaffId() != null ? request.getStaffId() : 0;
            safetyInspectionService.enableInspectionByCode(savedTicket.getTicketCode(), staffId);
        } else {
            safetyInspectionService.skipInspectionByCode(savedTicket.getTicketCode());
        }

        List<ServiceTicketResponse.Warning> warnings = buildAppointmentWarnings(booking);

        ServiceTicketResponse response = new ServiceTicketResponse();
        response.setServiceTicketId(savedTicket.getServiceTicketId());
        response.setTicketCode(savedTicket.getTicketCode());
        response.setBookingId(savedTicket.getBookingId());
        response.setVehicleId(savedTicket.getVehicleId());
        response.setTicketStatus(savedTicket.getTicketStatus());
        response.setCheckInNotes(savedTicket.getCheckInNotes());
        response.setImmutable(savedTicket.getImmutable());
        response.setCreatedAt(savedTicket.getCreatedAt());
        response.setUpdatedAt(savedTicket.getUpdatedAt());

        List<VehicleConditionPhoto> photos = photoRepo.findByServiceTicketId(savedTicket.getServiceTicketId());
        response.setPhotos(photoResponseMapper.toPhotoInfoList(photos));
        response.setWarnings(warnings);
        response.setSafetyInspectionEnabled(Boolean.TRUE.equals(request.getSafetyInspection()));

        return response;
    }

    @Transactional
    public ServiceTicket createServiceTicket(Integer bookingId, Integer vehicleId, Integer customerId, Integer createdBy) {
        return serviceTicketService.createServiceTicket(bookingId, vehicleId, customerId, createdBy);
    }

    @Transactional(readOnly = true)
    public ServiceTicket findByTicketCode(String ticketCode) {
        return serviceTicketService.findByTicketCode(ticketCode);
    }

    @Transactional(readOnly = true)
    public CustomerVehiclesResponse getCustomerVehicles(Integer customerId) {
        log.info("Getting vehicles for customer: {}", customerId);

        customerRepository.findById(customerId)
            .orElseThrow(() -> new CheckInException("Không tìm thấy khách hàng"));

        List<Vehicle> vehicles = vehicleRepository.findByCustomer_CustomerId(customerId);

        List<CustomerVehiclesResponse.VehicleInfo> vehicleInfos = vehicleMapper.toVehicleInfoList(vehicles);

        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            CustomerVehiclesResponse.VehicleInfo info = vehicleInfos.get(i);

            odometerRepo.findLatestByVehicleId(vehicle.getVehicleId())
                .ifPresent(r -> info.setLastOdometerReading(r.getReading()));

            List<ServiceTicket> allTickets = serviceTicketRepo.findAll();
            allTickets.stream()
                .filter(t -> t.getVehicleId().equals(vehicle.getVehicleId()))
                .max(java.util.Comparator.comparing(ServiceTicket::getCreatedAt))
                .ifPresent(t -> info.setLastServiceDate(t.getCreatedAt().toLocalDate()));
        }

        CustomerVehiclesResponse response = new CustomerVehiclesResponse();
        response.setCustomerId(customerId);
        response.setVehicles(vehicleInfos);

        log.info("Found {} vehicles for customer: {}", vehicleInfos.size(), customerId);
        return response;
    }

    /**
     * Complete check-in in single page form (all-in-one).
     */
    @Transactional
    public ServiceTicketResponse completeCheckInAll(CompleteCheckInAllRequest request) {
        log.info("Starting single-page check-in for booking: {}", request.getBookingId());

        if (request.getStaffId() == null) {
            throw new CheckInException("Thiếu thông tin nhân viên thực hiện check-in (staffId)");
        }

        // 1. Validate and get vehicle
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
            .orElseThrow(() -> new CheckInException("Không tìm thấy xe với ID: " + request.getVehicleId()));
        log.info("Using vehicle: vehicleId={}, licensePlate={}", vehicle.getVehicleId(), vehicle.getLicensePlate());

        // 2. Create Service Ticket
        ServiceTicket serviceTicket = serviceTicketService.createServiceTicket(
            request.getBookingId(), vehicle.getVehicleId(), request.getCustomerId(), request.getStaffId());
        log.info("Created service ticket: ticketCode={}", serviceTicket.getTicketCode());

        ServiceTicket ticketDomain = serviceTicketRepo.findByTicketCode(serviceTicket.getTicketCode())
            .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket vừa tạo"));
        final Integer createdTicketId = ticketDomain.getServiceTicketId();

        // 3. Upload license plate photo (optional)
        if (request.getLicensePlatePhoto() != null && !request.getLicensePlatePhoto().isEmpty()) {
            try {
                String photoUrl = imageUploadService.uploadImage(request.getLicensePlatePhoto(), FileUploadConstants.FOLDER_VEHICLE);
                VehicleConditionPhoto photo = new VehicleConditionPhoto();
                photo.setServiceTicketId(createdTicketId);
                photo.setCategory(PhotoCategory.LICENSE_PLATE);
                photo.setPhotoUrl(photoUrl);
                photo.setDescription("Ảnh biển số xe");
                photo.setUploadedAt(LocalDateTime.now());
                photo.setUploadedBy(request.getStaffId());
                photoRepo.save(photo);
                log.info("Uploaded license plate photo: {}", photoUrl);
            } catch (IOException e) {
                log.error("Failed to upload license plate photo", e);
                throw new CheckInException("Không thể upload ảnh biển số: " + e.getMessage());
            }
        }

        // 4. Upload vehicle condition photos
        int photoCount = 0;
        try {
            photoCount += savePhotoIfPresent(request.getPhotoFront(), PhotoCategory.FRONT, request.getPhotoFrontDescription(), createdTicketId, request.getStaffId());
            photoCount += savePhotoIfPresent(request.getPhotoRear(), PhotoCategory.BACK, request.getPhotoRearDescription(), createdTicketId, request.getStaffId());
            photoCount += savePhotoIfPresent(request.getPhotoLeftSide(), PhotoCategory.LEFT, request.getPhotoLeftSideDescription(), createdTicketId, request.getStaffId());
            photoCount += savePhotoIfPresent(request.getPhotoRightSide(), PhotoCategory.RIGHT, request.getPhotoRightSideDescription(), createdTicketId, request.getStaffId());
            photoCount += savePhotoIfPresent(request.getPhotoInterior(), PhotoCategory.OVERALL, request.getPhotoInteriorDescription(), createdTicketId, request.getStaffId());
            photoCount += savePhotoIfPresent(request.getPhotoDamage(), PhotoCategory.DAMAGE, request.getPhotoDamageDescription(), createdTicketId, request.getStaffId());
        } catch (IOException e) {
            log.error("Failed to upload vehicle condition photo", e);
            throw new CheckInException("Không thể upload ảnh xe: " + e.getMessage());
        }

        if (photoCount == 0) {
            throw new CheckInException("Phải upload ít nhất 1 ảnh tình trạng xe");
        }
        log.info("Uploaded {} vehicle condition photos", photoCount);

        // 5. Save odometer reading (optional)
        boolean rollbackDetected = false;
        Integer previousReadingValue = null;

        if (request.getOdometerReading() != null) {
            Optional<OdometerReading> previousReading = odometerRepo.findLatestByVehicleId(vehicle.getVehicleId());
            if (previousReading.isPresent()) {
                previousReadingValue = previousReading.get().getReading();
                if (request.getOdometerReading() < previousReadingValue) {
                    rollbackDetected = true;
                    log.warn("Odometer rollback detected: vehicleId={}, previous={}, current={}",
                        vehicle.getVehicleId(), previousReadingValue, request.getOdometerReading());
                }
            }

            OdometerReading reading = new OdometerReading();
            reading.setVehicleId(vehicle.getVehicleId());
            reading.setServiceTicketId(createdTicketId);
            reading.setReading(request.getOdometerReading());
            reading.setRecordedAt(LocalDateTime.now());
            reading.setRecordedBy(request.getStaffId());
            reading.setRollbackDetected(rollbackDetected);
            reading.setPreviousReading(previousReadingValue);
            odometerRepo.save(reading);

            log.info("Saved odometer reading: {} km, rollbackDetected={}", request.getOdometerReading(), rollbackDetected);
        }
        //11. set queue
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = now.toLocalDate().atTime(LocalTime.MAX);

        Integer currentMaxQueue = serviceTicketRepo.findMaxQueueNumberForToday(startOfToday, endOfToday);
        int nextQueueNumber = (currentMaxQueue == null) ? 1 : (currentMaxQueue + 1);

        // 6. Update Service Ticket to DRAFT
        ticketDomain.setTicketStatus(TicketStatus.DRAFT);
        ticketDomain.setCheckInNotes(request.getCheckInNotes());
        ticketDomain.setReceivedAt(LocalDateTime.now());
        ticketDomain.setUpdatedAt(LocalDateTime.now());
        ticketDomain.setQueueNumber(nextQueueNumber);
        ServiceTicket savedTicketAll = serviceTicketRepo.save(ticketDomain);
        log.info("Updated ticket status to DRAFT: {}", savedTicketAll.getTicketCode());

        // 7. Update booking status to DONE
        Booking booking = bookingService.findById(request.getBookingId());
        booking.setStatus(BookingStatus.DONE);
        bookingRepository.save(booking);
        log.info("Updated booking status to DONE: bookingId={}", request.getBookingId());

        // 8. Handle safety inspection choice
        if (Boolean.TRUE.equals(request.getSafetyInspection())) {
            safetyInspectionService.enableInspectionByCode(savedTicketAll.getTicketCode(), request.getStaffId());
            log.info("Safety inspection enabled for ticket: {}", savedTicketAll.getTicketCode());
        } else {
            safetyInspectionService.skipInspectionByCode(savedTicketAll.getTicketCode());
            log.info("Safety inspection skipped for ticket: {}", savedTicketAll.getTicketCode());
        }

        // 9. Build warnings
        List<ServiceTicketResponse.Warning> warnings = new ArrayList<>();

        if (rollbackDetected) {
            ServiceTicketResponse.Warning w = new ServiceTicketResponse.Warning();
            w.setCode("ODOMETER_ROLLBACK");
            w.setMessage(String.format(
                "CẢNH BÁO: Số công tơ mét hiện tại (%d km) nhỏ hơn lần trước (%d km). Có thể đã bị lùi công tơ mét.",
                request.getOdometerReading(), previousReadingValue));
            w.setSeverity("WARNING");
            warnings.add(w);
        }
        warnings.addAll(buildAppointmentWarnings(booking));

        // 10. Build response
        ServiceTicketResponse response = serviceTicketDtoMapper.toResponse(savedTicketAll);

        List<VehicleConditionPhoto> photos = photoRepo.findByServiceTicketId(savedTicketAll.getServiceTicketId());
        response.setPhotos(photoResponseMapper.toPhotoInfoList(photos));
        response.setWarnings(warnings);
        response.setSafetyInspectionEnabled(Boolean.TRUE.equals(request.getSafetyInspection()));

        log.info("Single-page check-in completed successfully: ticketCode={}, photoCount={}, warnings={}",
            savedTicketAll.getTicketCode(), photoCount, warnings.size());

        return response;
    }

    // === Private helpers ===

    private int savePhotoIfPresent(
            org.springframework.web.multipart.MultipartFile file,
            PhotoCategory category,
            String description,
            Integer ticketId,
            Integer staffId) throws IOException {
        if (file == null || file.isEmpty()) return 0;
        String url = imageUploadService.uploadImage(file, FileUploadConstants.FOLDER_VEHICLE);
        VehicleConditionPhoto photo = new VehicleConditionPhoto();
        photo.setServiceTicketId(ticketId);
        photo.setCategory(category);
        photo.setPhotoUrl(url);
        photo.setDescription(description);
        photo.setUploadedAt(LocalDateTime.now());
        photo.setUploadedBy(staffId);
        photoRepo.save(photo);
        log.info("Uploaded photo: category={}, url={}", category, url);
        return 1;
    }

    private List<ServiceTicketResponse.Warning> buildAppointmentWarnings(Booking booking) {
        List<ServiceTicketResponse.Warning> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (!booking.getScheduledDate().equals(today)) {
            ServiceTicketResponse.Warning w = new ServiceTicketResponse.Warning();
            w.setCode("APPOINTMENT_DATE_MISMATCH");
            w.setMessage(String.format("Khách hàng đến sai ngày hẹn. Ngày hẹn: %s, Ngày đến: %s",
                booking.getScheduledDate(), today));
            w.setSeverity("WARNING");
            warnings.add(w);
        }

        LocalTime scheduledTime = booking.getScheduledTime();
        int minutesDiff = Math.abs(now.toSecondOfDay() - scheduledTime.toSecondOfDay()) / 60;
        if (minutesDiff > 30) {
            ServiceTicketResponse.Warning w = new ServiceTicketResponse.Warning();
            w.setCode("APPOINTMENT_TIME_MISMATCH");
            w.setMessage(String.format("Khách hàng đến sai giờ hẹn. Giờ hẹn: %s, Giờ đến: %s",
                scheduledTime, now));
            w.setSeverity("INFO");
            warnings.add(w);
        }

        return warnings;
    }
}
