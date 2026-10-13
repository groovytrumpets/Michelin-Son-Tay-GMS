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
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import com.g42.platform.gms.service_ticket_management.domain.repository.OdometerReadingRepo;
import com.g42.platform.gms.customer.application.service.CustomerPhoneService;
import com.g42.platform.gms.customerimport.application.service.ImportNormalizer;


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
    private final CustomerProfileRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final OdometerReadingRepo odometerReadingRepo;
    private final CustomerPhoneService customerPhoneService;


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

        // Thông báo cho advisor hiện tại biết phiếu đã bị hủy
        ServiceTicketDetailResponse detail = manageService.getServiceTicketDetail(ticketCode);
        if (detail.getAdvisorId() != null) {
            staffNotifyService.createNotificationAssignAuto(
                detail.getAdvisorId(),
                "Phiếu dịch vụ đã bị hủy: " + ticketCode,
                "Phiếu dịch vụ " + ticketCode + " đã bị hủy. Vui lòng kiểm tra lại!",
                detail.getAdvisorId(),
                "http://localhost:5173/advisor/inspection"
            );
        }
        return detail;
    }


    /** Advisor cập nhật dịch vụ/báo giá khi ticket đang DRAFT. */
    @Transactional
    public ServiceTicketDetailResponse updateEstimate(String ticketCode, UpdateServiceTicketRequest request) {
        ServiceTicket ticket = findTicket(ticketCode);
        TicketStatus current = ticket.getTicketStatus();
        if (current == TicketStatus.PAID || current == TicketStatus.CANCELLED) {
            throw new CheckInException("Phiếu đang ở trạng thái "
                    + (current == TicketStatus.PAID ? "đã thanh toán" : "đã huỷ") + " nên không thể chỉnh sửa.");
        }
        // Phiếu đã báo xong (COMPLETED) vẫn phải sửa được thông tin đầu phiếu — nhất là ngày bàn
        // giao, vốn chỉ biết sau khi sửa xong — nhưng không được đổi danh sách dịch vụ nữa.
        if (current == TicketStatus.COMPLETED && request.getCatalogItemIds() != null) {
            throw new CheckInException("Phiếu đã hoàn thành sửa chữa nên không thể đổi danh sách dịch vụ.");
        }

        validateTicketTimes(ticket, request);

        // Update customerRequest
        if (request.getCustomerRequest() != null) {
            String customerRequest = request.getCustomerRequest().trim();
            if (customerRequest.length() > CUSTOMER_REQUEST_MAX_LENGTH) {
                throw CheckInException.field("customerRequest",
                        "Yêu cầu khách hàng tối đa " + CUSTOMER_REQUEST_MAX_LENGTH + " ký tự.");
            }
            ticket.setCustomerRequest(customerRequest);
        }

        // Giống lúc tạo lịch (/create-booking): chỉ bắt buộc có SĐT HOẶC biển số, họ tên/email/kiểu xe
        // được để trống. Chuỗi rỗng nghĩa là xoá giá trị (lưu NULL — cột phone/email là UNIQUE
        // nên không được lưu "" cho nhiều khách).
        com.g42.platform.gms.auth.entity.CustomerProfile customer = ticket.getCustomerId() == null ? null
                : customerRepository.findById(ticket.getCustomerId()).orElse(null);
        String phoneToDetach = null;
        if (request.getCustomerName() != null || request.getCustomerPhone() != null || request.getCustomerEmail() != null) {
            if (customer == null) {
                throw new CheckInException("Không tìm thấy khách hàng của phiếu này.");
            }
            if (request.getCustomerName() != null) {
                String name = blankToNull(request.getCustomerName());
                if (name != null && name.length() > NAME_MAX_LENGTH) {
                    throw CheckInException.field("customerName", "Họ tên tối đa " + NAME_MAX_LENGTH + " ký tự.");
                }
                customer.setFullName(name);
            }
            if (request.getCustomerPhone() != null) {
                String phone = resolveNewPhone(request.getCustomerPhone(), customer);
                if (phone != null && !phone.equals(customer.getPhone())) {
                    phoneToDetach = phone;
                }
                customer.setPhone(phone);
            }
            if (request.getCustomerEmail() != null) {
                customer.setEmail(resolveNewEmail(request.getCustomerEmail(), customer));
            }
        }

        if (request.getVehicleModel() != null) {
            String model = blankToNull(request.getVehicleModel());
            if (model != null && model.length() > VEHICLE_MODEL_MAX_LENGTH) {
                throw CheckInException.field("vehicleModel", "Loại & kiểu xe tối đa " + VEHICLE_MODEL_MAX_LENGTH + " ký tự.");
            }
        }

        String requestedPlate = request.getLicensePlate() == null ? null : blankToNull(request.getLicensePlate());
        if (requestedPlate != null && requestedPlate.length() > LICENSE_PLATE_MAX_LENGTH) {
            throw CheckInException.field("licensePlate", "Biển số tối đa " + LICENSE_PLATE_MAX_LENGTH + " ký tự.");
        }
        boolean willHavePlate = ticket.getVehicleId() != null || requestedPlate != null;
        boolean willHavePhone = customer != null && customer.getPhone() != null && !customer.getPhone().isBlank();
        if (!willHavePhone && !willHavePlate) {
            throw CheckInException.field("customerPhone", "Cần nhập số điện thoại hoặc biển số xe.");
        }
        if (request.getOdometerKm() != null
                && (request.getOdometerKm() < 0 || request.getOdometerKm() > ODOMETER_MAX_KM)) {
            throw CheckInException.field("odometerKm", "Số ki-lô-mét phải từ 0 đến 9.999.999.");
        }
        if (customer != null) {
            customerRepository.save(customer);
            // Số chính mới vốn là số phụ của chính khách này → bỏ khỏi danh sách phụ để một số
            // không nằm ở hai nơi (bất biến của CustomerPhoneService).
            if (phoneToDetach != null) {
                customerPhoneService.detachOtherPhone(customer.getCustomerId(), phoneToDetach);
            }
        }

        // Phiếu tiếp nhận chỉ bằng SĐT (chưa gắn xe) mà giờ mới nhập biển số → gắn xe cho phiếu:
        // dùng lại xe cùng biển của chính khách này nếu có, không thì tạo xe mới.
        if (ticket.getVehicleId() == null && requestedPlate != null) {
            com.g42.platform.gms.vehicle.entity.Vehicle attached = (customer == null ? java.util.Optional.<com.g42.platform.gms.vehicle.entity.Vehicle>empty()
                    : vehicleRepository.findByPlateForCustomer(requestedPlate, customer.getCustomerId()))
                    .orElseGet(() -> {
                        com.g42.platform.gms.vehicle.entity.Vehicle v = new com.g42.platform.gms.vehicle.entity.Vehicle();
                        v.setLicensePlate(requestedPlate);
                        v.setCustomer(customer);
                        return v;
                    });
            if (request.getVehicleModel() != null) {
                attached.setModel(blankToNull(request.getVehicleModel()));
            }
            attached = vehicleRepository.save(attached);
            ticket.setVehicleId(attached.getVehicleId());
        } else if (ticket.getVehicleId() != null && (request.getVehicleModel() != null || request.getLicensePlate() != null)) {
            // Update vehicle model and license plate
            com.g42.platform.gms.vehicle.entity.Vehicle vehicle = vehicleRepository.findById(ticket.getVehicleId())
                    .orElseThrow(() -> new CheckInException("Không tìm thấy xe"));
            if (request.getVehicleModel() != null) {
                vehicle.setModel(blankToNull(request.getVehicleModel()));
            }
            if (request.getLicensePlate() != null) {
                // Cột license_plate là NOT NULL nên xe đã gắn vào phiếu không xoá trắng biển được.
                if (requestedPlate == null) {
                    throw CheckInException.field("licensePlate", "Xe đã gắn với phiếu nên không thể để trống biển số.");
                }
                String newPlate = requestedPlate;
                // Xe dùng chung (vợ chồng, công ty) được phép trùng biển giữa các hồ sơ khác nhau
                // — xem changeset 039. Chỉ chặn khi chính chủ xe này đã có một xe khác cùng biển.
                Integer ownerId = vehicle.getCustomer() == null ? null : vehicle.getCustomer().getCustomerId();
                java.util.Optional<com.g42.platform.gms.vehicle.entity.Vehicle> sameOwnerVehicle =
                        vehicleRepository.findByPlateForCustomer(newPlate, ownerId);
                if (sameOwnerVehicle.isPresent() && !sameOwnerVehicle.get().getVehicleId().equals(vehicle.getVehicleId())) {
                    throw CheckInException.field("licensePlate", "Khách hàng này đã có một xe khác biển số " + newPlate + ".");
                }
                vehicle.setLicensePlate(newPlate);
            }
            vehicleRepository.save(vehicle);
        }

        // Update odometer reading
        if (request.getOdometerKm() != null) {
            if (ticket.getVehicleId() == null) {
                throw CheckInException.field("odometerKm", "Cần nhập biển số xe trước khi ghi số ki-lô-mét.");
            }
            java.util.List<com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading> readings =
                    odometerReadingRepo.findByServiceTicketId(ticket.getServiceTicketId());
            if (!readings.isEmpty()) {
                com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading reading = readings.get(0);
                reading.setReading(request.getOdometerKm());
                odometerReadingRepo.save(reading);
            } else {
                com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading newReading =
                        new com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading();
                newReading.setVehicleId(ticket.getVehicleId());
                newReading.setReading(request.getOdometerKm());
                newReading.setRecordedAt(LocalDateTime.now());
                newReading.setServiceTicketId(ticket.getServiceTicketId());
                odometerReadingRepo.save(newReading);
            }
        }

        // Update service ticket specific fields
        if (request.getReceivedAt() != null) {
            ticket.setReceivedAt(request.getReceivedAt());
        }
        if (request.getSafetyInspectionEnabled() != null) {
            ticket.setSafetyInspectionEnabled(request.getSafetyInspectionEnabled());
        }
        if (request.getEstimatedDeliveryAt() != null) {
            ticket.setEstimatedDeliveryAt(request.getEstimatedDeliveryAt());
        }
        if (request.getDeliveredAt() != null) {
            ticket.setDeliveredAt(request.getDeliveredAt());
        }

        // Save catalog items if booking exists
        if (ticket.getBookingId() != null && request.getCatalogItemIds() != null) {
            Booking booking = bookingRepository.findById(ticket.getBookingId())
                    .orElseThrow(() -> new CheckInException("Không tìm thấy booking"));
            booking.setCatalogItemIds(request.getCatalogItemIds());
            bookingRepository.save(booking);
        }

        serviceTicketRepo.save(ticket);
        log.info("Ticket {} details and estimate updated by advisor", ticketCode);
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

        // Thông báo cho advisor mới được giao phiếu
        String customerInfo = "";
        if (ticket.getCustomerId() != null) {
            var customerOpt = customerRepository.findById(ticket.getCustomerId());
            if (customerOpt.isPresent()) {
                var c = customerOpt.get();
                customerInfo = " - KH: " + c.getFullName() + " (" + c.getPhone() + ")";
            }
        }
        staffNotifyService.createNotificationAssignAuto(
            newAdvisorId,
            "Đã được giao phiếu: " + ticketCode + customerInfo,
            "Đã được giao phiếu: " + ticketCode + customerInfo + "; Vui lòng mở trang Điều phối dịch vụ để xác nhận!",
            newAdvisorId,
            "http://localhost:5173/advisor/inspection"
        );
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

        // Thông báo cho kỹ thuật viên bị hủy phân công
        staffNotifyService.createNotificationAssignAuto(
            technicianId,
            "Đã bị hủy phân công khỏi phiếu: " + ticketCode,
            "Bạn đã bị hủy phân công khỏi phiếu dịch vụ " + ticketCode + ". Vui lòng kiểm tra lại!",
            technicianId,
            "http://localhost:5173/advisor/inspection"
        );
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

        // Thông báo cho kỹ thuật viên mới được phân công
        String customerInfo = "";
        if (ticket.getCustomerId() != null) {
            var customerOpt = customerRepository.findById(ticket.getCustomerId());
            if (customerOpt.isPresent()) {
                var c = customerOpt.get();
                customerInfo = " - KH: " + c.getFullName() + " (" + c.getPhone() + ")";
            }
        }
        staffNotifyService.createNotificationAssignAuto(
            newTechnicianId,
            "Đã được phân công vào phiếu: " + ticketCode + customerInfo,
            "Bạn đã được phân công vào phiếu dịch vụ " + ticketCode + customerInfo + "; Vui lòng kiểm tra và xác nhận!",
            newTechnicianId,
            "http://localhost:5173/advisor/inspection"
        );
        // Thông báo cho kỹ thuật viên cũ bị thay thế
        staffNotifyService.createNotificationAssignAuto(
            oldTechnicianId,
            "Đã bị thay thế khỏi phiếu: " + ticketCode,
            "Bạn đã bị thay thế bởi kỹ thuật viên khác trong phiếu dịch vụ " + ticketCode + ".",
            newTechnicianId,
            "http://localhost:5173/advisor/inspection"
        );
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

    private static final int CUSTOMER_REQUEST_MAX_LENGTH = 1000;
    private static final int NAME_MAX_LENGTH = 255;
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int VEHICLE_MODEL_MAX_LENGTH = 255;
    private static final int LICENSE_PLATE_MAX_LENGTH = 30;
    private static final int ODOMETER_MAX_KM = 9_999_999;
    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * SĐT mới nhập ở đầu phiếu. Giữ nguyên số cũ thì không bắt định dạng (hồ sơ sổ cũ có thể
     * lưu số lạ); số mới phải là 10 số bắt đầu bằng 0 và chưa thuộc về khách khác — cột phone
     * là UNIQUE nên để lọt xuống DB sẽ thành 500 "Lỗi hệ thống".
     */
    private String resolveNewPhone(String raw, com.g42.platform.gms.auth.entity.CustomerProfile customer) {
        String trimmed = blankToNull(raw);
        if (trimmed == null) return null;
        if (trimmed.equals(customer.getPhone())) return customer.getPhone();
        String phone = ImportNormalizer.normalizePhone(trimmed);
        if (!ImportNormalizer.isValidPhone(phone)) {
            throw CheckInException.field("customerPhone", "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.");
        }
        if (phone.equals(customer.getPhone())) return phone;
        customerRepository.findByPhone(phone).ifPresent(owner -> {
            if (!owner.getCustomerId().equals(customer.getCustomerId())) {
                String name = owner.getFullName() == null || owner.getFullName().isBlank() ? "chưa có tên" : owner.getFullName();
                throw CheckInException.field("customerPhone", "Số điện thoại " + phone + " đã thuộc về khách hàng "
                        + name + ". Nếu là cùng một người, hãy gộp hai hồ sơ ở màn Gộp hồ sơ trùng.");
            }
        });
        return phone;
    }

    /** Email mới nhập ở đầu phiếu — lưu chữ thường như CustomerAuthService, và không được trùng khách khác (UNIQUE). */
    private String resolveNewEmail(String raw, com.g42.platform.gms.auth.entity.CustomerProfile customer) {
        String trimmed = blankToNull(raw);
        if (trimmed == null) return null;
        if (trimmed.equals(customer.getEmail())) return customer.getEmail();
        String email = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (email.length() > EMAIL_MAX_LENGTH) {
            throw CheckInException.field("customerEmail", "E-mail tối đa " + EMAIL_MAX_LENGTH + " ký tự.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw CheckInException.field("customerEmail", "E-mail không hợp lệ. Ví dụ đúng: ten@gmail.com");
        }
        if (customerRepository.existsByEmailIgnoreCaseAndCustomerIdNot(email, customer.getCustomerId())) {
            throw CheckInException.field("customerEmail", "E-mail " + email + " đã thuộc về một khách hàng khác.");
        }
        return email;
    }

    /**
     * Thứ tự thời gian của phiếu: dự kiến hoàn tất và ngày bàn giao không được trước ngày tiếp nhận.
     * Chỉ kiểm khi request có đổi một trong các mốc liên quan, để dữ liệu cũ đã lệch không chặn
     * người dùng sửa các ô khác.
     */
    private void validateTicketTimes(ServiceTicket ticket, UpdateServiceTicketRequest request) {
        LocalDateTime receivedAt = request.getReceivedAt() != null ? request.getReceivedAt() : ticket.getReceivedAt();
        LocalDateTime estimated = request.getEstimatedDeliveryAt() != null ? request.getEstimatedDeliveryAt() : ticket.getEstimatedDeliveryAt();
        LocalDateTime delivered = request.getDeliveredAt() != null ? request.getDeliveredAt() : ticket.getDeliveredAt();
        if (receivedAt == null) return;
        if (estimated != null && estimated.isBefore(receivedAt)
                && (request.getReceivedAt() != null || request.getEstimatedDeliveryAt() != null)) {
            throw CheckInException.field("estimatedDeliveryAt", "Dự kiến hoàn tất không được trước ngày tiếp nhận.");
        }
        if (delivered != null && delivered.isBefore(receivedAt)
                && (request.getReceivedAt() != null || request.getDeliveredAt() != null)) {
            throw CheckInException.field("deliveredAt", "Ngày bàn giao không được trước ngày tiếp nhận.");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

