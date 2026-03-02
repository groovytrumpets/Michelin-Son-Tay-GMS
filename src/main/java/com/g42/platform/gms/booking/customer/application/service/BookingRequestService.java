package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.booking.customer.api.dto.GuestBookingRequest;
import com.g42.platform.gms.booking.customer.domain.entity.BookingRequest;
import com.g42.platform.gms.booking.customer.domain.entity.BookingRequestDetail;
import com.g42.platform.gms.booking.customer.domain.entity.IpBlacklist;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking.customer.domain.exception.BookingException;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRequestDetailRepository;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRequestRepository;
import com.g42.platform.gms.booking.customer.domain.repository.IpBlacklistRepository;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingRequestService {
    
    private final BookingRequestRepository bookingRequestRepository;
    private final BookingRequestDetailRepository bookingRequestDetailRepository;
    private final IpBlacklistRepository ipBlacklistRepository;
    private final CustomerProfileRepository customerRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final StaffProfileRepo staffRepository;
    private final BookingCodeGenerator bookingCodeGenerator;
    
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    
    private static final int MAX_REQUESTS_PER_IP_PER_HOUR = 3;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    
    @Transactional
    public BookingRequest createGuestRequest(GuestBookingRequest request, String clientIp) {
        if (!isValidPhoneNumber(request.getPhone())) {
            throw new BookingException("Số điện thoại không hợp lệ.");
        }
        
        if (ipBlacklistRepository.existsByIpAddressAndIsActiveTrue(clientIp)) {
            throw new BookingException("Địa chỉ IP này đã bị chặn.");
        }
        
        String ipKey = "ip:" + clientIp;
        if (!checkRateLimit(ipKey, MAX_REQUESTS_PER_IP_PER_HOUR)) {
            throw new BookingException(
                "Bạn đã đặt quá nhiều lịch trong thời gian ngắn. Vui lòng thử lại sau 1 giờ."
            );
        }
        
        CustomerProfile customer = customerRepository.findByPhone(request.getPhone()).orElse(null);
        
        // Validate thời gian đặt lịch
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDateTime = LocalDateTime.of(
            request.getAppointmentDate(),
            request.getAppointmentTime()
        );
        
        // Check không được trong quá khứ
        if (scheduledDateTime.isBefore(now)) {
            throw new BookingException("Không thể đặt lịch trong quá khứ.");
        }
        
        // Check phải cách hiện tại ít nhất 2 giờ
        LocalDateTime minBookingTime = now.plusHours(2L);
        if (scheduledDateTime.isBefore(minBookingTime)) {
            throw new BookingException("Vui lòng đặt lịch trước ít nhất 2 giờ.");
        }
        
        // Guest không cần check slot availability - có thể đặt lịch bất kỳ
        
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setPhone(request.getPhone());
        bookingRequest.setFullName(request.getFullName());
        bookingRequest.setScheduledDate(request.getAppointmentDate());
        bookingRequest.setScheduledTime(request.getAppointmentTime());
        bookingRequest.setDescription(request.getUserNote());
        bookingRequest.setStatus(BookingRequestStatus.PENDING);
        bookingRequest.setIsGuest(true);
        
        Integer customerId = null;
        if (customer != null) {
            customerId = customer.getCustomerId();
        }
        bookingRequest.setCustomerId(customerId);
        bookingRequest.setClientIp(clientIp);
        bookingRequest.setCreatedAt(LocalDateTime.now());
        bookingRequest.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        // Generate unique request code: RQ_XXXXXX
        String requestCode = bookingCodeGenerator.generateCode(
            request.getAppointmentDate(),
            com.g42.platform.gms.common.enums.CodePrefix.REQUEST
        );
        bookingRequest.setRequestCode(requestCode);
        
        bookingRequest.initializeDefaults();
        BookingRequest savedRequest = bookingRequestRepository.save(bookingRequest);
        
        if (request.getSelectedServiceIds() != null && !request.getSelectedServiceIds().isEmpty()) {
            List<BookingRequestDetail> details = request.getSelectedServiceIds().stream()
                .map(itemId -> {
                    BookingRequestDetail detail = new BookingRequestDetail();
                    detail.setRequestId(savedRequest.getRequestId());
                    
                    Integer mappedItemId = null;
                    if (catalogItemRepository.findById(itemId).isPresent()) {
                        mappedItemId = itemId;
                    }
                    detail.setItemId(mappedItemId);
                    return detail;
                })
                .filter(d -> d.getItemId() != null)
                .collect(Collectors.toList());
            
            if (!details.isEmpty()) {
                bookingRequestDetailRepository.saveAll(details);
            }
        }
        
        log.info("Guest booking request created: requestId={}, phone={}", savedRequest.getRequestId(), request.getPhone());
        return savedRequest;
    }

    @Transactional
    public void addIpToBlacklist(String ipAddress, String reason, Integer staffId) {
        if (ipBlacklistRepository.existsByIpAddressAndIsActiveTrue(ipAddress)) {
            throw new BookingException("Địa chỉ IP này đã bị chặn.");
        }
        
        IpBlacklist blacklist = new IpBlacklist();
        blacklist.setIpAddress(ipAddress);
        blacklist.setReason(reason);
        blacklist.setIsActive(true);
        blacklist.setBlockedBy(staffId);
        
        blacklist.initializeDefaults();
        ipBlacklistRepository.save(blacklist);
        log.info("IP added to blacklist: ip={}, reason={}, staffId={}", ipAddress, reason, staffId);
    }

    @Transactional
    public void removeIpFromBlacklist(String ipAddress) {
        IpBlacklist blacklist = ipBlacklistRepository.findByIpAddressAndIsActiveTrue(ipAddress)
            .orElseThrow(() -> new BookingException("Địa chỉ IP này không có trong blacklist."));
        
        blacklist.setIsActive(false);
        ipBlacklistRepository.save(blacklist);
        log.info("IP removed from blacklist: ip={}", ipAddress);
    }

    @Transactional
    public void autoAddIpToBlacklistIfSpam(String ipAddress, String reason) {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<BookingRequest> rejectedRequests = bookingRequestRepository.findRejectedByIpSince(ipAddress, last24Hours);
        
        if (rejectedRequests.size() >= 5) {
            if (!ipBlacklistRepository.existsByIpAddressAndIsActiveTrue(ipAddress)) {
                IpBlacklist blacklist = new IpBlacklist();
                blacklist.setIpAddress(ipAddress);
                
                String blacklistReason = "Tự động chặn do spam (5+ request bị từ chối trong 24h)";
                if (reason != null) {
                    blacklistReason = reason;
                }
                blacklist.setReason(blacklistReason);
                blacklist.setIsActive(true);
                blacklist.initializeDefaults();
                ipBlacklistRepository.save(blacklist);
                log.warn("Auto-blacklisted IP due to spam: ip={}, rejectedCount={}", ipAddress, rejectedRequests.size());
            }
        }
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
    
    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("^0[0-9]{9,10}$");
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
            return Duration.between(firstRequestTime, LocalDateTime.now())
                .compareTo(RATE_LIMIT_WINDOW) < 0;
        }
    }
}
