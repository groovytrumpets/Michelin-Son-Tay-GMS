package com.g42.platform.gms.booking.customer.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.service.JwtUtilCustomer;
import com.g42.platform.gms.booking.customer.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.entity.Booking;
import com.g42.platform.gms.booking.customer.entity.BookingStatus;
import com.g42.platform.gms.booking.customer.exception.BookingException;
import com.g42.platform.gms.booking.customer.repository.BookingRepository;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepo;
    private final CustomerProfileRepository customerRepo;
    private final CatalogItemRepository catalogRepo;
    private final JwtUtilCustomer jwtUtilCustomer;

    @Transactional
    public BookingResponse createCustomerBooking(CustomerBookingRequest request, String token) {
        // Lấy customerId từ JWT token
        Claims claims = jwtUtilCustomer.extractClaims(token);
        Integer customerId = claims.get("customerId", Integer.class);
        
        if (customerId == null) {
            throw new BookingException("Không tìm thấy thông tin khách hàng trong token.");
        }
        
        CustomerProfile customer = customerRepo.findById(customerId)
            .orElseThrow(() -> new BookingException("Không tìm thấy thông tin khách hàng."));
        
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setScheduledDate(request.getAppointmentDate());
        booking.setScheduledTime(request.getAppointmentTime());
        booking.setDescription(request.getUserNote() != null ? request.getUserNote() : "");
        booking.setIsGuest(false);
        booking.setStatus(BookingStatus.PENDING);
        
        // Thêm danh sách dịch vụ nếu có
        if (request.getSelectedServiceIds() != null && !request.getSelectedServiceIds().isEmpty()) {
            booking.setServices(catalogRepo.findAllById(request.getSelectedServiceIds()));
        } else {
            booking.setServices(new ArrayList<>());
        }
        
        Booking savedBooking = bookingRepo.save(booking);
        log.info("Customer booking created: bookingId={}, customerId={}", savedBooking.getBookingId(), customerId);
        
        return BookingResponse.from(savedBooking);
    }

    public List<BookingResponse> getCustomerBookings(String token) {
        // Lấy danh sách booking của customer đã đăng nhập
        Claims claims = jwtUtilCustomer.extractClaims(token);
        Integer customerId = claims.get("customerId", Integer.class);
        
        if (customerId == null) {
            throw new BookingException("Không tìm thấy thông tin khách hàng trong token.");
        }
        
        List<Booking> bookings = bookingRepo.findByCustomer_CustomerIdOrderByScheduledDateDescScheduledTimeDesc(customerId);
        
        return bookings.stream()
            .map(BookingResponse::from)
            .collect(Collectors.toList());
    }
}