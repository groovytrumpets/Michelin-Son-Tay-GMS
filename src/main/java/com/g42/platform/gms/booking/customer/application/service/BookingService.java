package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.api.dto.CustomerBookingRequest;
import com.g42.platform.gms.booking.customer.api.dto.ModifyBookingRequest;
import com.g42.platform.gms.booking.customer.api.dto.StaffDirectBookingRequest;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import com.g42.platform.gms.booking.customer.domain.exception.BookingException;
import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.booking_management.api.internal.BookingManageInternalApi;
import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.common.exception.CodeGenerationException;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.booking.customer.domain.repository.IpBlacklistRepository;
import com.g42.platform.gms.catalog.infrastructure.repository.CatalogItemRepository;
import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
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

/**
 * Service xử lý nghiệp vụ đặt lịch (Booking) cho khách hàng
 * 
 * Chức năng chính:
 * - Tạo booking mới (customer hoặc staff tạo)
 * - Xem danh sách booking của customer
 * - Sửa đổi booking (thay đổi thời gian, dịch vụ)
 * - Hủy booking
 * 
 * Business Rules:
 * - Customer đã login phải đặt trước ít nhất 2 giờ
 * - Staff/Receptionist có thể đặt ngay tức khắc (bypass 2 giờ)
 * - Chỉ được sửa/hủy trước 2 giờ so với giờ hẹn
 * - Rate limiting: tối đa 100 booking/giờ/customer (giá trị cao để tránh block nhầm)
 * - IP blacklist checking (chỉ áp dụng cho customer đã login)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    // === CONSTANTS - Các hằng số cấu hình ===
    
    /** Thời hạn cho phép sửa/hủy booking (2 giờ trước giờ hẹn) */
    private static final long MODIFY_LIMIT_HOURS = 2L;
    
    /** Thời gian tối thiểu phải đặt trước (2 giờ) - chỉ áp dụng cho customer */
    private static final long MIN_BOOKING_LEAD_TIME_HOURS = 2L;
    
    /** Thời lượng mặc định nếu không có service nào được chọn (60 phút) */
    private static final int DEFAULT_DURATION_MINUTES = 60;
    
    /** Giới hạn số lần đặt lịch (100 lần/giờ/customer - giá trị cao để tránh block nhầm) */
    private static final int MAX_REQUESTS_PER_CUSTOMER_PER_HOUR = 100;

    // === DEPENDENCIES - Các dependency injection ===
    
    private final BookingRepository bookingRepository;
    private final CustomerProfileRepository customerRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final SlotService slotService;
    private final IpBlacklistRepository ipBlacklistRepository;
    private final BookingCodeGenerator bookingCodeGenerator;
    private final com.g42.platform.gms.catalog.infrastructure.repository.ComboItemRepository comboItemRepository;

    /** Cache để tracking rate limit (in-memory, sẽ reset khi restart server) */
    private final Map<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    private final BookingManageInternalApi bookingManageInternalApi;
    private final EstimateInternalApi estimateInternalApi;

    // ========================================
    // CREATE BOOKING - Tạo booking mới
    // ========================================

    /**
     * Tạo booking cho customer đã login (customer tự đặt qua app/web)
     * 
     * Đặc điểm:
     * - Customer phải đăng nhập trước
     * - Phải đặt trước ít nhất 2 giờ (MIN_BOOKING_LEAD_TIME_HOURS)
     * - Trạng thái CONFIRMED ngay (không qua PENDING)
     * - Generate mã booking mới (MST_XXXXXX)
     * - Check slot availability trước khi tạo
     * - Áp dụng rate limiting (100 booking/giờ/customer)
     * - Áp dụng IP blacklist checking
     * 
     * Flow:
     * 1. Check IP blacklist
     * 2. Check rate limiting (100 booking/giờ/customer)
     * 3. Validate customer exists
     * 4. Build booking object với status = CONFIRMED
     * 5. Validate thời gian đặt lịch (phải trước ít nhất 2 giờ)
     * 6. Check slot availability
     * 7. Generate booking code (MST_XXXXXX)
     * 8. Save booking vào database
     * 9. Reserve slot cho booking này
     * 
     * @param request Thông tin booking (appointmentDate, appointmentTime, userNote, selectedServiceIds)
     * @param customerId ID của khách hàng (từ JWT token)
     * @param clientIp IP của client (để check blacklist)
     * @return Booking đã được tạo với status = CONFIRMED
     * @throws BookingException nếu vi phạm business rules
     */
    @Transactional
    public Booking createCustomerBooking(CustomerBookingRequest request, Integer customerId, String clientIp) {
        // === 1. CHECK IP BLACKLIST ===
        // Check IP blacklist
        if (ipBlacklistRepository.existsByIpAddressAndIsActiveTrue(clientIp)) {
            throw new BookingException("Địa chỉ IP này đã bị chặn.");
        }

        // === 2. RATE LIMITING ===
        // Rate limiting per customer (tối đa 100 booking/giờ - giá trị cao để tránh block nhầm)
        String customerKey = "customer:" + customerId;
        if (!checkRateLimit(customerKey, MAX_REQUESTS_PER_CUSTOMER_PER_HOUR)) {
            throw new BookingException(
                    "Bạn đã đặt quá nhiều lịch trong thời gian ngắn. Vui lòng thử lại sau 1 giờ."
            );
        }

        // === 3. VALIDATE CUSTOMER ===
        CustomerProfile customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BookingException("Không tìm thấy thông tin khách hàng."));

        // === 4. BUILD BOOKING OBJECT VỚI STATUS = CONFIRMED ===
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
            booking.setCatalogItemIds(request.getSelectedServiceIds());
        } else {
            booking.setCatalogItemIds(new ArrayList<>());
        }



        // === 5. VALIDATE THỜI GIAN (PHẢI TRƯỚC ÍT NHẤT 2 GIỜ) ===
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDateTime = LocalDateTime.of(
                booking.getScheduledDate(),
                booking.getScheduledTime()
        );

        // Check không được trong quá khứ
        if (scheduledDateTime.isBefore(now)) {
            throw new BookingException("Không thể đặt lịch trong quá khứ.");
        }

        // Check phải cách hiện tại ít nhất 2 giờ (MIN_BOOKING_LEAD_TIME_HOURS)
        LocalDateTime minBookingTime = now.plusHours(MIN_BOOKING_LEAD_TIME_HOURS);
        if (scheduledDateTime.isBefore(minBookingTime)) {
            throw new BookingException("Vui lòng đặt lịch trước ít nhất 2 giờ.");
        }

        // === 6. CHECK SLOT AVAILABILITY ===
        int estimatedDuration = calculateEstimatedDuration(booking.getCatalogItemIds());

        boolean slotAvailable = slotService.isSlotAvailable(
                booking.getScheduledDate(),
                booking.getScheduledTime(),
                estimatedDuration,
                null
        );
        if (!slotAvailable) {
            throw new BookingException("Khung giờ này đã đầy, vui lòng chọn giờ khác.");
        }

        // === 7. GENERATE BOOKING CODE (MST_XXXXXX) ===
        try {
            String bookingCode = bookingCodeGenerator.generateCode(LocalDate.now(), CodePrefix.BOOKING);
            booking.setBookingCode(bookingCode);
            log.info("Generated booking code: {}", bookingCode);
        } catch (CodeGenerationException e) {
            log.error("Failed to generate booking code: {}", e.getMessage());
            throw new BookingException("Không thể tạo mã booking: " + e.getMessage());
        }

        // === 8. SAVE BOOKING VÀO DATABASE ===
        booking.initializeDefaults();
        Integer maxQueueOrder = bookingManageInternalApi.findLatestQueueByBookingReservation(booking);
        booking.setQueueOrder(maxQueueOrder!=null?maxQueueOrder+1:1);
        Booking savedBooking = bookingRepository.save(booking);

        // === 9. RESERVE SLOT CHO BOOKING NÀY ===
        slotService.reserveForBooking(savedBooking.getBookingId(), estimatedDuration);

        log.info("Customer booking created: bookingId={}, bookingCode={}, customerId={}",
                savedBooking.getBookingId(), savedBooking.getBookingCode(), customerId);

        return savedBooking;
    }

    /**
     * Tạo booking trực tiếp bởi Staff/Receptionist (không qua BookingRequest)
     * 
     * Đặc điểm:
     * - KHÔNG bị ràng buộc thời gian 2 giờ (có thể đặt ngay tức khắc)
     * - Trạng thái CONFIRMED luôn (không qua PENDING)
     * - Tự động tạo/tìm customer account từ phone number
     * - Check slot availability trước khi tạo
     * - KHÔNG áp dụng rate limiting
     * - KHÔNG áp dụng IP blacklist checking
     * 
     * Flow:
     * 1. Tìm customer account theo phone, nếu không có thì tạo mới
     * 2. Build booking object với status = CONFIRMED
     * 3. Validate thời gian (chỉ check không được quá khứ, KHÔNG check 2 giờ)
     * 4. Check slot availability
     * 5. Generate booking code (MST_XXXXXX)
     * 6. Save booking vào database
     * 7. Reserve slot cho booking này
     * 
     * @param request Thông tin booking (phone, fullName, appointmentDate, appointmentTime, userNote, selectedServiceIds)
     * @return Booking đã được tạo với status = CONFIRMED
     * @throws BookingException nếu vi phạm business rules
     */
    @Transactional
    public Booking createDirectBookingByStaff(StaffDirectBookingRequest request) {

        
        // === 1. TÌM HOẶC TẠO CUSTOMER ACCOUNT ===
        CustomerProfile customer = customerRepository.findByPhone(request.getPhone()).orElse(null);
        
        Integer customerId;
        if (customer == null) {
            // Tạo customer account mới
            CustomerProfile newCustomer = new CustomerProfile();
            newCustomer.setPhone(request.getPhone());
            newCustomer.setFullName(request.getFullName());
            newCustomer.setCreatedAt(LocalDateTime.now());
            
            CustomerProfile savedCustomer = customerRepository.save(newCustomer);
            customerId = savedCustomer.getCustomerId();
            log.info("Created new customer account: customerId={}, phone={}", customerId, request.getPhone());
        } else {
            customerId = customer.getCustomerId();
            
            // Update tên nếu khác
            if (!request.getFullName().equals(customer.getFullName())) {
                customer.setFullName(request.getFullName());
                customerRepository.save(customer);
                log.info("Updated customer name: customerId={}, newName={}", customerId, request.getFullName());
            }
        }
        
        // === 2. BUILD BOOKING OBJECT VỚI STATUS = CONFIRMED ===
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
        booking.setEstimateId(request.getEstimateId());

        if (request.getSelectedServiceIds() != null && !request.getSelectedServiceIds().isEmpty()) {
            booking.setCatalogItemIds(request.getSelectedServiceIds());
        } else {
            booking.setCatalogItemIds(new ArrayList<>());
        }
        if (request.getEstimateId() != null) {
            Estimate estimate = estimateInternalApi.findById(request.getEstimateId());
            if (estimate == null) {
                throw new BookingException("Khong tim thay bao gia voi estimateId: " + request.getEstimateId());
            }
        }
        
        // === 3. VALIDATE THỜI GIAN (CHỈ CHECK KHÔNG ĐƯỢC QUÁ KHỨ) ===
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDateTime = LocalDateTime.of(request.getAppointmentDate(), request.getAppointmentTime());

        // Staff chỉ cần check không được trong quá khứ (KHÔNG check 2 giờ)
        if (scheduledDateTime.isBefore(now)) {
            throw new BookingException("Không thể đặt lịch trong quá khứ.");
        }
        
        // === 4. CHECK SLOT AVAILABILITY ===
        int estimatedDuration = calculateEstimatedDuration(booking.getCatalogItemIds());
        
        boolean slotAvailable = slotService.isSlotAvailable(
                booking.getScheduledDate(),
                booking.getScheduledTime(),
                estimatedDuration,
                null
        );
        if (!slotAvailable) {
            throw new BookingException("Khung giờ này đã đầy, vui lòng chọn giờ khác.");
        }
        
        // === 5. GENERATE BOOKING CODE (MST_XXXXXX) ===
        try {
            String bookingCode = bookingCodeGenerator.generateCode(LocalDate.now(), CodePrefix.BOOKING);
            booking.setBookingCode(bookingCode);
            log.info("Generated booking code: {}", bookingCode);
        } catch (CodeGenerationException e) {
            log.error("Failed to generate booking code: {}", e.getMessage());
            throw new BookingException("Không thể tạo mã booking: " + e.getMessage());
        }
        
        // === 6. SAVE BOOKING VÀO DATABASE ===
        booking.initializeDefaults();
        Integer maxQueueOrder = bookingManageInternalApi.findLatestQueueByBookingReservation(booking);
        booking.setQueueOrder(maxQueueOrder!=null?maxQueueOrder+1:1);
        System.out.println("DEBUG: "+booking.getQueueOrder());
        Booking savedBooking = bookingRepository.save(booking);
        
        // === 7. RESERVE SLOT CHO BOOKING NÀY ===
        slotService.reserveForBooking(savedBooking.getBookingId(), estimatedDuration);
        
        log.info("Direct booking created by staff: bookingId={}, bookingCode={}, customerId={}",
                savedBooking.getBookingId(), savedBooking.getBookingCode(), customerId);
        //todo:update remind if exist
        if (request.getReminderId()!=null) {
            estimateInternalApi.updateBookingToRemindById(request.getReminderId(), savedBooking.getBookingId());
        }
        return savedBooking;
    }

    // ========================================
    // QUERY BOOKING - Truy vấn booking
    // ========================================

    /**
     * Lấy danh sách booking của customer (sắp xếp theo ngày giảm dần)
     */
    public List<Booking> getCustomerBookings(Integer customerId) {
        return bookingRepository.findByCustomerIdOrderByDateDesc(customerId);
    }

    /**
     * Tìm booking theo booking code
     */
    public Booking findByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BookingException("Không tìm thấy booking với mã: " + bookingCode));
    }

    /**
     * Tìm booking theo ID
     */
    public Booking findById(Integer bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Không tìm thấy booking với ID: " + bookingId));
    }

    // ========================================
    // MODIFY BOOKING - Sửa đổi booking
    // ========================================

    /**
     * Sửa đổi booking (thay đổi thời gian, dịch vụ, ghi chú)
     * 
     * Business Rules:
     * - Chỉ được sửa trước 2 giờ so với giờ hẹn
     * - Thời gian mới phải trước ít nhất 2 giờ
     * - Chỉ sửa được booking ở trạng thái PENDING hoặc CONFIRMED
     * 
     * @param bookingId ID của booking
     * @param request Thông tin cần sửa
     * @param customerId ID của customer (để verify ownership)
     * @return Booking đã được sửa
     */
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

        // Tính duration từ catalogItemIds CUỐI CÙNG (sau khi update)
        List<Integer> finalCatalogItemIds = booking.getCatalogItemIds();
        if (request.getNewServiceIds() != null && !request.getNewServiceIds().isEmpty()) {
            finalCatalogItemIds = request.getNewServiceIds();
        }
        int estimatedDuration = calculateEstimatedDuration(finalCatalogItemIds);

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
            booking.setCatalogItemIds(request.getNewServiceIds());
        }

        booking.initializeDefaults();
        Booking saved = bookingRepository.save(booking);

        if (timeChanged) {
            slotService.reserveForBooking(saved.getBookingId(), estimatedDuration);
        }

        log.info("Customer booking modified: bookingId={}, customerId={}", saved.getBookingId(), customerId);
        return saved;
    }

    // ========================================
    // CANCEL BOOKING - Hủy booking
    // ========================================

    /**
     * Hủy booking
     * 
     * Business Rules:
     * - Chỉ được hủy trước 2 giờ so với giờ hẹn
     * - Chỉ hủy được booking ở trạng thái PENDING hoặc CONFIRMED
     * - Không hủy được booking đã qua thời gian hẹn
     * 
     * @param bookingId ID của booking
     * @param customerId ID của customer (để verify ownership)
     */
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

    // ========================================
    // HELPER METHODS - Các method hỗ trợ
    // ========================================

    /**
     * Tính tổng thời lượng ước tính dựa trên danh sách catalog item IDs
     * 
     * Logic:
     * - Nếu không có service nào => trả về DEFAULT_DURATION_MINUTES (60 phút)
     * - Nếu có service => tính tổng duration của từng service
     * - Nếu có combo => tính duration của tất cả service trong combo
     * 
     * @param catalogItemIds Danh sách ID của catalog items (services/combos)
     * @return Tổng thời lượng (phút)
     */
    private int calculateEstimatedDuration(List<Integer> catalogItemIds) {
        if (catalogItemIds == null || catalogItemIds.isEmpty()) {
            return DEFAULT_DURATION_MINUTES;
        }

        List<CatalogItemJpaEntity> items = catalogItemRepository.findAllById(catalogItemIds);

        int totalMinutes = 0;
        for (CatalogItemJpaEntity item : items) {
            int itemDuration = calculateItemDuration(item);
            totalMinutes = totalMinutes + itemDuration;
        }

        if (totalMinutes == 0) {
            return DEFAULT_DURATION_MINUTES;
        }

        return totalMinutes;
    }

    /**
     * Tính duration của một catalog item (có thể là SERVICE hoặc COMBO)
     */
    private int calculateItemDuration(CatalogItemJpaEntity item) {
        if (item == null) {
            return 0;
        }

        String itemType = item.getItemType();

        if ("SERVICE".equals(itemType)) {
            return getServiceDuration(item);
        }

        if ("COMBO".equals(itemType)) {
            return getComboDuration(item.getItemId());
        }

        return 0;
    }

    /**
     * Lấy duration của một service
     */
    private int getServiceDuration(CatalogItemJpaEntity item) {
        if (item.getServiceService() == null) {
            return 0;
        }

        Integer estimateTime = item.getServiceService().getEstimateTime();
        if (estimateTime == null || estimateTime <= 0) {
            return 0;
        }

        return estimateTime;
    }

    /**
     * Tính tổng duration của một combo (tổng duration của tất cả services trong combo)
     */
    private int getComboDuration(Integer comboId) {
        List<com.g42.platform.gms.booking.customer.infrastructure.entity.ComboItemJpaEntity> comboItems =
                comboItemRepository.findByComboId(comboId);

        int totalDuration = 0;
        for (com.g42.platform.gms.booking.customer.infrastructure.entity.ComboItemJpaEntity comboItem : comboItems) {
            CatalogItemJpaEntity includedItem = comboItem.getIncludedItem();
            boolean isServiceItem = includedItem != null && "SERVICE".equals(includedItem.getItemType());
            if (!isServiceItem) {
                continue;
            }

            int serviceDuration = getServiceDuration(includedItem);
            int quantity = getQuantityOrDefault(comboItem.getQuantity());
            totalDuration = totalDuration + (serviceDuration * quantity);
        }

        return totalDuration;
    }

    /**
     * Lấy quantity, nếu null hoặc <= 0 thì trả về 1
     */
    private int getQuantityOrDefault(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return 1;
        }
        return quantity;
    }

    /**
     * Check rate limiting (in-memory cache)
     * 
     * Logic:
     * - Nếu chưa có record hoặc đã hết window (1 giờ) => tạo mới, cho phép
     * - Nếu đã đạt maxRequests => không cho phép
     * - Nếu còn trong window và chưa đạt max => increment count, cho phép
     * 
     * @param key Key để tracking (vd: "customer:123")
     * @param maxRequests Số request tối đa trong 1 giờ
     * @return true nếu cho phép, false nếu vượt quá giới hạn
     */
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

    /**
     * Inner class để tracking rate limit
     * Lưu số lần request và thời điểm request đầu tiên
     */
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
