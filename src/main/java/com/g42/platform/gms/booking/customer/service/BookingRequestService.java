package com.g42.platform.gms.booking.customer.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.booking.customer.dto.BookingRequestResponse;
import com.g42.platform.gms.booking.customer.dto.GuestBookingRequest;
import com.g42.platform.gms.booking.customer.entity.BookingRequest;
import com.g42.platform.gms.booking.customer.entity.BookingRequestDetail;
import com.g42.platform.gms.booking.customer.entity.BookingRequestStatus;
import com.g42.platform.gms.booking.customer.entity.IpBlacklist;
import com.g42.platform.gms.booking.customer.repository.BookingRequestDetailRepository;
import com.g42.platform.gms.booking.customer.repository.BookingRequestRepository;
import com.g42.platform.gms.booking.customer.repository.IpBlacklistRepository;
import com.g42.platform.gms.booking.customer.exception.BookingException;
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
    
    private final BookingRequestRepository bookingRequestRepo;
    private final BookingRequestDetailRepository bookingRequestDetailRepo;
    private final IpBlacklistRepository ipBlacklistRepo;
    private final CustomerProfileRepository customerRepo;
    private final CatalogItemRepository catalogRepo;
    private final StaffProfileRepo staffRepo;
    
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    
    private static final int MAX_REQUESTS_PER_IP_PER_HOUR = 3;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    
    @Transactional
    public BookingRequestResponse createGuestRequest(GuestBookingRequest request, String clientIp) {
        // Tạo booking request cho khách chưa đăng nhập, cần nhân viên xác nhận
        if (!isValidPhoneNumber(request.getPhone())) {
            throw new BookingException("Số điện thoại không hợp lệ.");
        }
        
        if (ipBlacklistRepo.existsByIpAddressAndIsActiveTrue(clientIp)) {
            throw new BookingException("Địa chỉ IP này đã bị chặn.");
        }
        
        String ipKey = "ip:" + clientIp;
        if (!checkRateLimit(ipKey, MAX_REQUESTS_PER_IP_PER_HOUR)) {
            throw new BookingException(
                "Bạn đã đặt quá nhiều lịch trong thời gian ngắn. Vui lòng thử lại sau 1 giờ."
            );
        }
        
        CustomerProfile customer = customerRepo.findByPhone(request.getPhone()).orElse(null);
        
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setPhone(request.getPhone());
        bookingRequest.setFullName(request.getFullName());
        bookingRequest.setScheduledDate(request.getAppointmentDate());
        bookingRequest.setScheduledTime(request.getAppointmentTime());
        bookingRequest.setDescription(request.getUserNote());
        bookingRequest.setStatus(BookingRequestStatus.PENDING);
        bookingRequest.setIsGuest(true);
        bookingRequest.setCustomer(customer);
        bookingRequest.setClientIp(clientIp);
        bookingRequest.setCreatedAt(LocalDateTime.now());
        bookingRequest.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        BookingRequest savedRequest = bookingRequestRepo.save(bookingRequest);
        
        if (request.getSelectedServiceIds() != null && !request.getSelectedServiceIds().isEmpty()) {
            List<BookingRequestDetail> details = request.getSelectedServiceIds().stream()
                .map(itemId -> {
                    BookingRequestDetail detail = new BookingRequestDetail();
                    detail.setRequest(savedRequest);
                    catalogRepo.findById(itemId).ifPresent(detail::setItem);
                    return detail;
                })
                .filter(d -> d.getItem() != null)
                .collect(Collectors.toList());
            
            if (!details.isEmpty()) {
                bookingRequestDetailRepo.saveAll(details);
            }
        }
        
        log.info("Guest booking request created: requestId={}, phone={}", savedRequest.getRequestId(), request.getPhone());
        return BookingRequestResponse.from(savedRequest);
    }

    @Transactional
    public void addIpToBlacklist(String ipAddress, String reason, Integer staffId) {
        // Thêm IP vào blacklist để chặn spam
        if (ipBlacklistRepo.existsByIpAddressAndIsActiveTrue(ipAddress)) {
            throw new BookingException("Địa chỉ IP này đã bị chặn.");
        }
        
        IpBlacklist blacklist = new IpBlacklist();
        blacklist.setIpAddress(ipAddress);
        blacklist.setReason(reason);
        blacklist.setIsActive(true);
        if (staffId != null) {
            staffRepo.findById(staffId).ifPresent(blacklist::setBlockedBy);
        }
        
        ipBlacklistRepo.save(blacklist);
        log.info("IP added to blacklist: ip={}, reason={}, staffId={}", ipAddress, reason, staffId);
    }

    @Transactional
    public void removeIpFromBlacklist(String ipAddress) {
        // Gỡ IP khỏi blacklist
        IpBlacklist blacklist = ipBlacklistRepo.findByIpAddressAndIsActiveTrue(ipAddress)
            .orElseThrow(() -> new BookingException("Địa chỉ IP này không có trong blacklist."));
        
        blacklist.setIsActive(false);
        ipBlacklistRepo.save(blacklist);
        log.info("IP removed from blacklist: ip={}", ipAddress);
    }

    @Transactional
    public void autoAddIpToBlacklistIfSpam(String ipAddress, String reason) {
        // Tự động chặn IP nếu có 5+ request bị từ chối trong 24h
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<BookingRequest> rejectedRequests = bookingRequestRepo.findRejectedByIpSince(ipAddress, last24Hours);
        
        if (rejectedRequests.size() >= 5) {
            if (!ipBlacklistRepo.existsByIpAddressAndIsActiveTrue(ipAddress)) {
                IpBlacklist blacklist = new IpBlacklist();
                blacklist.setIpAddress(ipAddress);
                blacklist.setReason(reason != null ? reason : "Tự động chặn do spam (5+ request bị từ chối trong 24h)");
                blacklist.setIsActive(true);
                ipBlacklistRepo.save(blacklist);
                log.warn("Auto-blacklisted IP due to spam: ip={}, rejectedCount={}", ipAddress, rejectedRequests.size());
            }
        }
    }
    
    // Kiểm tra giới hạn số request trong khoảng thời gian
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
    
    // Kiểm tra định dạng số điện thoại Việt Nam
    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("^0[0-9]{9,10}$");
    }
    
    // Lưu thông tin rate limit cho mỗi IP
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
