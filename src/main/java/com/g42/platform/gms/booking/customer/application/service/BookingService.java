package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.service.JwtUtilCustomer;
import com.g42.platform.gms.booking.customer.api.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.api.dto.ModifyBookingRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final long MODIFY_LIMIT_HOURS = 2L;
    private static final int MAX_BOOKINGS_PER_SLOT = 3;

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

    @Transactional
    public Booking modifyCustomerBooking(Integer bookingId, ModifyBookingRequest request, String token) {
        Claims claims = jwtUtilCustomer.extractClaims(token);
        Integer customerId = claims.get("customerId", Integer.class);

        if (customerId == null) {
            throw new BookingException("Không tìm thấy thông tin khách hàng trong token.");
        }

        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customerId)
            .orElseThrow(() -> new BookingException("Không tìm thấy booking của bạn."));

        BookingStatus currentStatus = booking.getStatus();
        if (currentStatus == BookingStatus.CANCELLED || currentStatus == BookingStatus.NOT_ARRIVED) {
            throw new BookingException("Booking đã kết thúc, không thể thay đổi.");
        }
        if (currentStatus != BookingStatus.PENDING && currentStatus != BookingStatus.CONFIRMED) {
            throw new BookingException("Trạng thái hiện tại không cho phép thay đổi.");
        }

        LocalDate newDate = booking.getScheduledDate();
        if (request.getNewAppointmentDate() != null) {
            newDate = request.getNewAppointmentDate();
        }

        LocalTime newTime = booking.getScheduledTime();
        if (request.getNewAppointmentTime() != null) {
            newTime = request.getNewAppointmentTime();
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime scheduledDateTime = LocalDateTime.of(newDate, newTime);
        if (scheduledDateTime.isBefore(now)) {
            throw new BookingException("Không thể chọn thời gian trong quá khứ.");
        }

        LocalDateTime modifyDeadline = scheduledDateTime.minusHours(MODIFY_LIMIT_HOURS);

        if (now.isAfter(modifyDeadline)) {
            throw new BookingException("Đã quá thời hạn cho phép thay đổi lịch.");
        }

        boolean slotAvailable = isSlotAvailable(newDate, newTime, booking.getBookingId());
        if (!slotAvailable) {
            throw new BookingException("Khung giờ mới đã đầy, vui lòng chọn giờ khác.");
        }

        booking.setScheduledDate(newDate);
        booking.setScheduledTime(newTime);

        if (request.getNewUserNote() != null) {
            String oldDescription = booking.getDescription();
            String newDescription = request.getNewUserNote();

            StringBuilder builder = new StringBuilder();
            if (oldDescription != null && !oldDescription.isBlank()) {
                builder.append(oldDescription).append(" | ");
            }
            builder.append(newDescription);

            booking.setDescription(builder.toString());
        }

        if (request.getNewServiceIds() != null && !request.getNewServiceIds().isEmpty()) {
            booking.setServiceIds(request.getNewServiceIds());
        }

        if (currentStatus == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.PENDING);
        }

        booking.initializeDefaults();
        Booking saved = bookingRepository.save(booking);
        log.info("Customer booking modified: bookingId={}, customerId={}", saved.getBookingId(), customerId);
        return saved;
    }

    @Transactional
    public void cancelCustomerBooking(Integer bookingId, String token) {
        Claims claims = jwtUtilCustomer.extractClaims(token);
        Integer customerId = claims.get("customerId", Integer.class);

        if (customerId == null) {
            throw new BookingException("Không tìm thấy thông tin khách hàng trong token.");
        }

        Booking booking = bookingRepository.findByIdAndCustomerId(bookingId, customerId)
            .orElseThrow(() -> new BookingException("Không tìm thấy booking của bạn."));

        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.CANCELLED || status == BookingStatus.NOT_ARRIVED) {
            throw new BookingException("Booking đã kết thúc, không thể hủy.");
        }
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            throw new BookingException("Trạng thái hiện tại không cho phép hủy.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDateTime = LocalDateTime.of(
            booking.getScheduledDate(),
            booking.getScheduledTime()
        );

        if (scheduledDateTime.isBefore(now)) {
            throw new BookingException("Không thể hủy lịch đã qua thời gian hẹn.");
        }

        LocalDateTime cancelDeadline = scheduledDateTime.minusHours(MODIFY_LIMIT_HOURS);
        if (now.isAfter(cancelDeadline)) {
            throw new BookingException("Đã quá thời hạn cho phép hủy lịch.");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);
        log.info("Customer booking cancelled: bookingId={}, customerId={}", booking.getBookingId(), customerId);

        // TODO: khi triển khai slot chi tiết, release slot tại đây
    }

    private boolean isSlotAvailable(LocalDate date, LocalTime time, Integer excludeBookingId) {
        List<BookingStatus> activeStatuses = Arrays.asList(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED
        );

        long count = bookingRepository.countByDateTimeAndStatuses(date, time, activeStatuses);

        if (excludeBookingId != null) {
            // Khi dùng countBy... theo JPA, booking hiện tại vẫn nằm trong count.
            // Để đơn giản, cho phép giữ nguyên giờ cũ kể cả khi đủ MAX_BOOKINGS_PER_SLOT.
            Booking existing = bookingRepository.findById(excludeBookingId).orElse(null);
            if (existing != null
                && existing.getScheduledDate().equals(date)
                && existing.getScheduledTime().equals(time)) {
                return true;
            }
        }

        return count < MAX_BOOKINGS_PER_SLOT;
    }
}
