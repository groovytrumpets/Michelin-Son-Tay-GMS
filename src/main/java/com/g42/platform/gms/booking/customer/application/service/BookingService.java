package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.service.JwtUtilCustomer;
import com.g42.platform.gms.booking.customer.api.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import com.g42.platform.gms.booking.customer.domain.exception.BookingException;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
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

    private final BookingRepository bookingRepository;
    private final CustomerProfileRepository customerRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final JwtUtilCustomer jwtUtilCustomer;

    @Transactional
    public Booking createCustomerBooking(CustomerBookingRequest request, String token) {
        Claims claims = jwtUtilCustomer.extractClaims(token);
        Integer customerId = claims.get("customerId", Integer.class);
        
        if (customerId == null) {
            throw new BookingException("Không tìm thấy thông tin khách hàng trong token.");
        }
        
        CustomerProfile customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new BookingException("Không tìm thấy thông tin khách hàng."));
        
        Booking booking = new Booking();
        booking.setCustomerId(customerId);
        booking.setScheduledDate(request.getAppointmentDate());
        booking.setScheduledTime(request.getAppointmentTime());
        
        String description = "";
        if (request.getUserNote() != null) {
            description = request.getUserNote();
        }
        booking.setDescription(description);
        booking.setIsGuest(false);
        booking.setStatus(BookingStatus.PENDING);
        
        if (request.getSelectedServiceIds() != null && !request.getSelectedServiceIds().isEmpty()) {
            booking.setServiceIds(request.getSelectedServiceIds());
        } else {
            booking.setServiceIds(new ArrayList<>());
        }
        
        booking.initializeDefaults();
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Customer booking created: bookingId={}, customerId={}", savedBooking.getBookingId(), customerId);
        
        return savedBooking;
    }

    public List<Booking> getCustomerBookings(String token) {
        Claims claims = jwtUtilCustomer.extractClaims(token);
        Integer customerId = claims.get("customerId", Integer.class);
        
        if (customerId == null) {
            throw new BookingException("Không tìm thấy thông tin khách hàng trong token.");
        }
        
        return bookingRepository.findByCustomerIdOrderByDateDesc(customerId);
    }
}
