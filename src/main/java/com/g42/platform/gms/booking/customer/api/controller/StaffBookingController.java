package com.g42.platform.gms.booking.customer.api.controller;

import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.auth.dto.CustomerLookupResponse;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.api.dto.StaffDirectBookingRequest;
import com.g42.platform.gms.booking.customer.api.mapper.BookingDtoMapper;
import com.g42.platform.gms.booking.customer.application.service.BookingService;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cho Staff/Receptionist tạo booking trực tiếp
 * 
 * Endpoint:
 * - POST /api/booking/staff/create-direct - Tạo booking trực tiếp (không qua BookingRequest)
 * 
 * Đặc điểm:
 * - Không bị ràng buộc thời gian 2 giờ
 * - Trạng thái CONFIRMED luôn
 * - Tự động tạo/tìm customer account từ phone
 */
@RestController
@RequestMapping("/api/booking/staff")
@RequiredArgsConstructor
public class StaffBookingController {

    private final BookingService bookingService;
    private final BookingDtoMapper dtoMapper;
    private final CustomerProfileRepository customerRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final VehicleRepository vehicleRepository;

    /**
     * Lookup customer info bằng số điện thoại HOẶC biển số xe (ít nhất 1 trong 2)
     *
     * Use case: Khi receptionist nhập số điện thoại hoặc biển số xe, tự động điền tên nếu customer đã tồn tại
     *
     * @param phone Số điện thoại cần tra cứu (tuỳ chọn)
     * @param licensePlate Biển số xe cần tra cứu (tuỳ chọn) — dùng khi không có/không nhớ số điện thoại
     * @return Thông tin customer (nếu có) hoặc empty response
     */
    @GetMapping("/customer-lookup")
    // @PreAuthorize("hasAnyRole('STAFF', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<CustomerLookupResponse>> lookupCustomer(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String licensePlate
    ) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedPlate = licensePlate == null ? "" : licensePlate.trim().toUpperCase();

        CustomerProfile customer = normalizedPhone.isBlank()
                ? null
                : customerRepository.findByPhone(normalizedPhone).orElse(null);

        if (customer == null && !normalizedPlate.isBlank()) {
            customer = vehicleRepository.findByLicensePlate(normalizedPlate)
                    .map(Vehicle::getCustomer)
                    .orElse(null);
        }

        CustomerLookupResponse response;
        if (customer != null) {
            long count = serviceTicketRepository.countActiveTicketsByCustomerId(customer.getCustomerId());
            response = new CustomerLookupResponse(
                    customer.getCustomerId(),
                    customer.getPhone(),
                    customer.getFullName(),
                    customer.getEmail(),
                    true,
                    count
            );
        } else {
            response = new CustomerLookupResponse(
                    null,
                    normalizedPhone,
                    null,
                    null,
                    false
            );
        }

        return ResponseEntity.ok(ApiResponses.success(response));
    }

    /**
     * Tạo booking trực tiếp cho khách hàng (bởi staff/receptionist)
     * 
     * Business Rules:
     * - Không bị ràng buộc thời gian 2 giờ (có thể đặt ngay)
     * - Trạng thái CONFIRMED luôn (không qua PENDING)
     * - Tự động tạo customer account nếu chưa có
     * - Check slot availability
     * 
     * @param request Thông tin booking (phone, fullName, appointmentDate, appointmentTime, userNote, selectedServiceIds)
     * @return Booking đã được tạo
     */
    @PostMapping("/create-direct")
//    @PreAuthorize("hasAnyRole('STAFF', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> createDirectBooking(
            @RequestBody @Valid StaffDirectBookingRequest request
    ) {
        Booking domain = bookingService.createDirectBookingByStaff(request);

        BookingResponse response = dtoMapper.toResponse(domain);

        // Populate customer info — ưu tiên lấy phone thật của customer đã resolve (có thể tạo/tra
        // ra từ biển số xe chứ không nhất thiết trùng request.getPhone()).
        String resolvedPhone = customerRepository.findById(domain.getCustomerId())
                .map(CustomerProfile::getPhone)
                .orElse(request.getPhone());
        response.setPhone(resolvedPhone);
        response.setCustomerName(request.getFullName());
        
        return ResponseEntity.ok(ApiResponses.success(response));
    }
}
