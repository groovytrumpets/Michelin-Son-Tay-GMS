package com.g42.platform.gms.service_ticket_management.application.service;


import com.g42.platform.gms.auth.api.internal.CustomerInternalApi;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.catalog.infrastructure.repository.CatalogItemRepository;
import com.g42.platform.gms.common.service.ExcelService;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceQueueResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketListResponse;
import com.g42.platform.gms.service_ticket_management.api.mapper.ServiceTicketDtoMapper;
import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspection;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentErrorCode;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentException;
import com.g42.platform.gms.service_ticket_management.domain.exception.CheckInException;
import com.g42.platform.gms.service_ticket_management.domain.repository.OdometerReadingRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.VehicleConditionPhotoRepo;
import com.g42.platform.gms.service_ticket_management.api.mapper.ServiceTicketListMapper;
import com.g42.platform.gms.service_ticket_management.api.mapper.ServiceTicketDetailMapper;
import com.g42.platform.gms.vehicle.api.internal.VehicleInternalApi;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


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
    private final ServiceTicketDtoMapper serviceTicketDtoMapper;
    private final CustomerInternalApi customerInternalApi;
    private final VehicleInternalApi vehicleInternalApi;
    private final EstimateInternalApi estimateInternalApi;
    private final SafetyInspectionService safetyInspectionService;

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


    /**
     * Get paginated list of service tickets assigned to a specific staff.
     */
    @Transactional(readOnly = true)
    public Page<ServiceTicketListResponse> getServiceTicketListByAssignedStaff(
            Integer staffId,
            int page,
            int size,
            LocalDate date,
            TicketStatus status,
            String search) {


        log.info(
                "Getting assigned service ticket list: staffId={}, page={}, size={}, date={}, status={}, search={}",
                staffId, page, size, date, status, search
        );


        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<ServiceTicket> ticketPage = serviceTicketRepo.findByAssignedStaff(staffId, status, date, search, pageable);
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
        response.setEstimatedDeliveryAt(ticket.getEstimatedDeliveryAt());
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

        // Advisor info — lấy từ assignment
        List<com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto> assignments =
                ticketAssignmentService.getAssignments(ticket.getServiceTicketId());
        assignments.stream()
                .filter(a -> "ADVISOR".equals(a.getRoleInTicket()))
                .filter(a -> "PENDING".equals(a.getStatus()) || "ACTIVE".equals(a.getStatus()))
                .findFirst()
                .ifPresent(a -> {
                    response.setAdvisorId(a.getStaffId());
                    response.setAdvisorName(a.getFullName());
                });


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
     * Advisor/lễ tân đặt thời gian hẹn lấy xe dự kiến.
     * Lưu vào estimated_delivery_at.
     */
    @Transactional
    public ServiceTicketDetailResponse setEstimatedDelivery(String ticketCode, LocalDateTime estimatedDeliveryAt) {
        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
                .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));
        ticket.setEstimatedDeliveryAt(estimatedDeliveryAt);
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → estimatedDeliveryAt={}", ticketCode, estimatedDeliveryAt);
        return getServiceTicketDetail(ticketCode);
    }

    /**
     * Lễ tân xác nhận khách đã lấy xe thật sự.
     * Update delivered_at = now().
     */
    @Transactional
    public ServiceTicketDetailResponse confirmDelivered(String ticketCode) {
        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
                .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));
        ticket.setDeliveredAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        serviceTicketRepo.save(ticket);
        log.info("Ticket {} → deliveredAt={}", ticketCode, ticket.getDeliveredAt());
        return getServiceTicketDetail(ticketCode);
    }


    /**
     * Lễ tân thay đổi advisor cho ticket.
     * Điều kiện: ticket phải DRAFT và advisor hiện tại phải PENDING (chưa bắt đầu làm việc).
     */
    @Transactional
    public ServiceTicketDetailResponse changeAdvisor(String ticketCode, Integer newAdvisorId, String note) {
        log.info("Receptionist changing advisor for ticket: {}, newAdvisorId: {}", ticketCode, newAdvisorId);

        ServiceTicket ticket = serviceTicketRepo.findByTicketCode(ticketCode)
                .orElseThrow(() -> new CheckInException("Không tìm thấy service ticket: " + ticketCode));

        if (ticket.getTicketStatus() != TicketStatus.CREATED) {
            throw new CheckInException("Lễ tân chỉ được đổi advisor khi phiếu đang CREATED. Hiện tại: " + ticket.getTicketStatus());
        }

        // Delegate sang TicketAssignmentService — sẽ validate advisor PENDING bên trong
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

    public List<ServiceQueueResponse> setswapQueueByServiceTicketIds(Integer serviceTicketId1, Integer serviceTicketId2) {
        ServiceTicket serviceTicket1 = serviceTicketRepo.findByServiceTicketId(serviceTicketId1);
        ServiceTicket serviceTicket2 = serviceTicketRepo.findByServiceTicketId(serviceTicketId2);
        compare2serviceTicket(serviceTicket1,serviceTicket2);
        Integer swap = serviceTicket1.getQueueNumber();
        serviceTicket1.setQueueNumber(serviceTicket2.getQueueNumber());
        serviceTicket2.setQueueNumber(swap);
        serviceTicketRepo.save(serviceTicket1);
        serviceTicketRepo.save(serviceTicket2);
        return getServiceTicketsByDate(serviceTicket1.getReceivedAt());
    }

    private List<ServiceQueueResponse> getServiceTicketsByDate(LocalDateTime receivedAt) {
        List<ServiceTicket> serviceTickets = serviceTicketRepo.findAllByDate(receivedAt);
        return serviceTickets.stream().map(serviceTicketDtoMapper::toQueueDto).toList();
    }

    private void compare2serviceTicket(ServiceTicket serviceTicket1, ServiceTicket serviceTicket2) {
        //todo: check validate service ticket
        if (!serviceTicket1.getReceivedAt().toLocalDate().equals(serviceTicket2.getReceivedAt().toLocalDate())) {
            throw new AssignmentException("Swap service ticket not match create Date", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
        }
        if (serviceTicket1.getQueueNumber()==null || serviceTicket2.getQueueNumber()==null) {
            throw new AssignmentException("Swap service ticket queue null", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
        }
    }

    public ServiceTicketListResponse updateServiceTicketStatus(Integer serviceTicketId, TicketStatus status) {
        ServiceTicket serviceTicket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        serviceTicket.setTicketStatus(status);
        ServiceTicket savedServiceTicket = serviceTicketRepo.save(serviceTicket);
        return serviceTicketDtoMapper.toDto(savedServiceTicket);
    }
    @Transactional
    public byte[] exportTicketToExcel(LocalDate startDate, LocalDate endDate){
        if (startDate == null) {
            startDate = LocalDate.of(2020, 1, 1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        List<ServiceTicket> serviceTickets = serviceTicketRepo.findBetween(start,end);

        List<Integer> customerIds = serviceTickets.stream().map(ServiceTicket::getCustomerId).distinct().toList();
        List<Integer> vehicleIds = serviceTickets.stream().map(ServiceTicket::getVehicleId).distinct().toList();
        List<Integer> ticketIds = serviceTickets.stream().map(ServiceTicket::getServiceTicketId).toList();

        List<CustomerProfile> customerProfiles = customerInternalApi.findAllByIds(customerIds);
        List<Vehicle> vehicles = vehicleInternalApi.findAllByIds(vehicleIds);
        List<Estimate> estimates = estimateInternalApi.findAllByServiceTicketId(ticketIds);

        Map<Integer, CustomerProfile> customerMap = customerProfiles.stream()
                .collect(Collectors.toMap(CustomerProfile::getCustomerId, c -> c));
        Map<Integer, Vehicle> vehicleMap = vehicles.stream()
                .collect(Collectors.toMap(Vehicle::getVehicleId, v -> v));
        Map<Integer, Estimate> estimateMap = estimates.stream()
                .collect(Collectors.toMap(
                        Estimate::getServiceTicketId,
                        e -> e,
                        (existing, replacement) -> replacement
                ));

        for (ServiceTicket ticket : serviceTickets) {

        }
        String[] headers={
                "STT",
                "Họ tên",
                "Số điện thoại",
                "Tổng tiền",
                "Note cầu của khách",
                "Biển số",
                "Hãng xe",
                "Kiểu xe",
                "Kilomet",
                "Ngày nhận xe",
                "Ngày giao xe",
                "Lưu ý",



        };
        int[] stt = {1};
        return ExcelService.exportToExcel(serviceTickets, headers, ticket -> {
            // Lấy thông tin từ Map
            CustomerProfile customer = customerMap.get(ticket.getCustomerId());
            Vehicle vehicle = vehicleMap.get(ticket.getVehicleId());
            Estimate estimate = estimateMap.get(ticket.getServiceTicketId());

            // Khởi tạo mảng Object có độ dài đúng 12 phần tử khớp với headers
            return new Object[]{
                    stt[0]++,                                                                   // 0: STT
                    customer != null ? customer.getFullName() : "",                             // 1: Họ tên
                    customer != null ? customer.getPhone() : "",                                // 2: SĐT
                    estimate != null ? estimate.getTotalPrice() : 0,                            // 3: Tổng tiền
                    ticket.getCustomerRequest() != null ? ticket.getCustomerRequest() : "",     // 4: Note
                    vehicle != null ? vehicle.getLicensePlate() : "",                           // 5: Biển số
                    vehicle != null ? vehicle.getBrand() : "",                                  // 6: Hãng xe
                    vehicle != null ? vehicle.getModel() : "",                                  // 7: Kiểu xe
                    "",                                                                         // 8: Kilomet (nếu có truy vấn odometer thì điền, không thì để rỗng)
                    ticket.getReceivedAt() != null ? ticket.getReceivedAt().toLocalDate() : "", // 9: Ngày nhận
                    ticket.getDeliveredAt() != null ? ticket.getDeliveredAt().toLocalDate() : "",// 10: Ngày giao
                    ticket.getCheckInNotes() != null ? ticket.getCheckInNotes() : ""            // 11: Lưu ý
            };
        });
    }

    public String getCustomerPerviousRecomment(Integer serviceTicketId) {
        ServiceTicket serviceTicket = serviceTicketRepo.findByServiceTicketId(serviceTicketId);
        CustomerProfile customerProfile = customerInternalApi.findById(serviceTicket.getCustomerId());
        /* todo: get previous custumer service ticket id */
        Integer previousId = serviceTicketRepo.findPerviousCustomerService(customerProfile.getCustomerId(),serviceTicketId
        ,serviceTicket.getVehicleId()).getServiceTicketId();
        System.out.println("DEBUG: "+previousId);
        if (previousId != null) {
            //todo: get recomment
            SafetyInspection safetyInspection = safetyInspectionService.findByServiceTicketId(previousId);
            if (safetyInspection != null) {
            return safetyInspection.getGeneralNotes();
            }
        }
        return null;
    }

    public List<ServiceTicketListResponse> getServiceTicketsHistory(Integer customerId, Integer vehicleId) {
        if (customerId == null || vehicleId == null) {
            throw new AssignmentException("CustomerId or VehicleId are null!",AssignmentErrorCode.BAD_REQUEST);
        }
        List<ServiceTicket> serviceTickets = serviceTicketRepo.findByCustomerAndVehicle(customerId,vehicleId);
        return serviceTickets.stream().map(serviceTicketDtoMapper::toDto).toList();
    }
}



