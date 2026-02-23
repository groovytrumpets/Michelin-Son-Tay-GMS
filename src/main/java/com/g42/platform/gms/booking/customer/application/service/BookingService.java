package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.api.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.api.dto.ModifyBookingRequest;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import com.g42.platform.gms.booking.customer.domain.exception.BookingException;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.booking.customer.domain.repository.IpBlacklistRepository;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final long MODIFY_LIMIT_HOURS = 2L;
    private static final long MIN_BOOKING_LEAD_TIME_HOURS = 2L;
    private static final int DEFAULT_DURATION_MINUTES = 60;

    private final BookingRepository bookingRepository;
    private final CustomerProfileRepository customerRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final SlotService slotService;
    private final IpBlacklistRepository ipBlacklistRepository;
    
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    
    private static final int MAX_REQUESTS_PER_CUSTOMER_PER_HOUR = 5;

    @Transactional
    public Booking createCustomerBooking(CustomerBookingRequest request, Integer customerId, String clientIp) {
        // Check IP blacklist
        if (ipBlacklistRepository.existsByIpAddressAndIsActiveTrue(clientIp)) {
            throw new BookingException("Địa chỉ IP này đã bị chặn.");
        }
        
        // Rate limiting per customer
        String customerKey = "customer:" + customerId;
        if (!checkRateLimit(customerKey, MAX_REQUESTS_PER_CUSTOMER_PER_HOUR)) {
            throw new BookingException(
                "Bạn đã đặt quá nhiều lịch trong thời gian ngắn. Vui lòng thử lại sau 1 giờ."
            );
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
        booking.setStatus(BookingStatus.CONFIRMED);
        
        if (request.getSelectedServiceIds() != null && !request.getSelectedServiceIds().isEmpty()) {
            booking.setServiceIds(request.getSelectedServiceIds());
        } else {
            booking.setServiceIds(new ArrayList<>());
        }
        
        // Validate thời gian đặt lịch
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDateTime = LocalDateTime.of(
            booking.getScheduledDate(),
            booking.getScheduledTime()
        );
        
        // Check không được trong quá khứ
        if (scheduledDateTime.isBefore(now)) {
            throw new BookingException("Không thể đặt lịch trong quá khứ.");
        }
        
        // Check phải cách hiện tại ít nhất 2 giờ
        LocalDateTime minBookingTime = now.plusHours(MIN_BOOKING_LEAD_TIME_HOURS);
        if (scheduledDateTime.isBefore(minBookingTime)) {
            throw new BookingException("Vui lòng đặt lịch trước ít nhất 2 giờ.");
        }
        
        int estimatedDuration = calculateEstimatedDuration(booking.getServiceIds());
        
        boolean slotAvailable = slotService.isSlotAvailable(
                booking.getScheduledDate(),
                booking.getScheduledTime(),
                estimatedDuration,
                null
        );
        if (!slotAvailable) {
            throw new BookingException("Khung giờ này đã đầy, vui lòng chọn giờ khác.");
        }
        
        booking.initializeDefaults();
        Booking savedBooking = bookingRepository.save(booking);
        
        slotService.reserveForBooking(savedBooking.getBookingId(), estimatedDuration);
        
        log.info("Customer booking created: bookingId={}, customerId={}", savedBooking.getBookingId(), customerId);
        
        return savedBooking;
    }

    public List<Booking> getCustomerBookings(Integer customerId) {
        return bookingRepository.findByCustomerIdOrderByDateDesc(customerId);
    }

    @Transactional
    public Booking modifyCustomerBooking(Integer bookingId, ModifyBookingRequest request, Integer customerId) {
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

        // Check phải cách hiện tại ít nhất 2 giờ
        LocalDateTime minBookingTime = now.plusHours(MIN_BOOKING_LEAD_TIME_HOURS);
        if (scheduledDateTime.isBefore(minBookingTime)) {
            throw new BookingException("Vui lòng chọn lịch trước ít nhất 2 giờ.");
        }

        LocalDateTime modifyDeadline = scheduledDateTime.minusHours(MODIFY_LIMIT_HOURS);

        if (now.isAfter(modifyDeadline)) {
            throw new BookingException("Đã quá thời hạn cho phép thay đổi lịch.");
        }

        LocalDate oldDate = booking.getScheduledDate();
        LocalTime oldTime = booking.getScheduledTime();
        boolean timeChanged = !oldDate.equals(newDate) || !oldTime.equals(newTime);

        // Tính duration từ serviceIds CUỐI CÙNG (sau khi update)
        List<Integer> finalServiceIds = booking.getServiceIds();
        if (request.getNewServiceIds() != null && !request.getNewServiceIds().isEmpty()) {
            finalServiceIds = request.getNewServiceIds();
        }
        int estimatedDuration = calculateEstimatedDuration(finalServiceIds);
        
        if (timeChanged) {
            boolean slotAvailable = slotService.isSlotAvailable(
                    newDate, newTime, estimatedDuration, booking.getBookingId()
            );
            if (!slotAvailable) {
                throw new BookingException("Khung giờ mới đã đầy, vui lòng chọn giờ khác.");
            }
            
            slotService.releaseForBooking(booking.getBookingId());
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
        
        if (timeChanged) {
            slotService.reserveForBooking(saved.getBookingId(), estimatedDuration);
        }
        
        log.info("Customer booking modified: bookingId={}, customerId={}", saved.getBookingId(), customerId);
        return saved;
    }

    @Transactional
    public void cancelCustomerBooking(Integer bookingId, Integer customerId) {
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
        
        slotService.releaseForBooking(booking.getBookingId());
        
        log.info("Customer booking cancelled: bookingId={}, customerId={}", booking.getBookingId(), customerId);
    }

    private int calculateEstimatedDuration(List<Integer> serviceIds) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            return DEFAULT_DURATION_MINUTES;
        }
        
        // TODO: Tính duration từ catalog_item.estimated_duration_minutes
        // Hiện tại tạm thời return default
        return DEFAULT_DURATION_MINUTES;
    }
    
    private boolean checkRateLimit(String key, int maxRequests) {
        RateLimitInfo info = rateLimitCache.get(key);
        if (info == null || !info.isWithinWindow()) {
            rateLimitCache.put(key, new RateLimitInfo(1));
            return true;
        }
        if (info.getCount() >= maxRequests) {
            return false;
        }
        info.increment();
        return true;
    }
    
    @lombok.Data
    private static class RateLimitInfo {
        private int count;
        private LocalDateTime firstRequestTime;
        
        public RateLimitInfo(int count) {
            this.count = count;
            this.firstRequestTime = LocalDateTime.now();
        }
        
        public void increment() {
            this.count++;
        }
        
        public boolean isWithinWindow() {
            return LocalDateTime.now().isBefore(firstRequestTime.plusHours(1));
        }
    }
}
