package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.booking.customer.api.dto.TimeSlotResponse;
import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.entity.SlotReservation;
import com.g42.platform.gms.booking.customer.domain.entity.TimeSlot;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.booking.customer.domain.repository.SlotReservationRepository;
import com.g42.platform.gms.booking.customer.domain.repository.TimeSlotRepository;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingConfigJpa;
import com.g42.platform.gms.booking.customer.infrastructure.entity.WorkingHoursJpa;
import com.g42.platform.gms.booking.customer.infrastructure.repository.BookingConfigJpaRepo;
import com.g42.platform.gms.booking.customer.infrastructure.repository.WorkingHoursJpaRepo;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý nghiệp vụ quản lý slot (khung giờ) và reservation (đặt chỗ)
 *
 * Chức năng chính:
 * - Check slot availability (kiểm tra slot còn trống không)
 * - Reserve slot cho booking (đặt chỗ)
 * - Release slot khi hủy/sửa booking (giải phóng chỗ)
 * - Lấy danh sách slots available cho customer (có filter lead-time)
 * - Lấy danh sách slots available cho staff (không filter lead-time)
 *
 * Slot System:
 * - Mỗi slot có capacity (sức chứa)
 * - Mỗi booking chiếm 1 hoặc nhiều slots (tùy duration)
 * - Slot được chia theo slotDurationMinutes (đọc từ BookingConfig, mục Cấu hình hệ thống)
 * - Slot chỉ hợp lệ nếu nằm trong giờ hoạt động của xưởng ngày đó (WorkingHoursConfig), không rơi vào giờ nghỉ trưa
 * - Sử dụng Pessimistic Locking để tránh race condition
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final SlotReservationRepository reservationRepository;
    private final BookingRepository bookingRepository;
    private final BookingConfigJpaRepo bookingConfigJpaRepo;
    private final WorkingHoursJpaRepo workingHoursJpaRepo;

    /**
     * Check xem slot có available không (có đủ chỗ trống không)
     *
     * Logic:
     * 1. Tính số blocks cần thiết dựa trên duration
     * 2. Với mỗi block, check:
     *    - Có nằm trong giờ hoạt động của xưởng (không phải giờ nghỉ) không
     *    - Slot config có tồn tại và active không
     *    - Số reservation hiện tại < capacity không
     * 3. Nếu tất cả blocks đều OK => available
     *
     * NOTE: Method này chỉ dùng để query hiển thị (getAvailableSlots).
     * Khi tạo booking, dùng checkAndReserve() để đảm bảo atomic.
     *
     * @param date Ngày đặt
     * @param startTime Giờ bắt đầu
     * @param estimatedDurationMinutes Thời lượng ước tính (phút)
     * @param excludeBookingId ID của booking cần exclude (dùng khi modify booking)
     * @return true nếu available, false nếu đã đầy
     */
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(LocalDate date,
                                   LocalTime startTime,
                                   int estimatedDurationMinutes,
                                   Integer excludeBookingId) {

        int slotDurationMinutes = getSlotDurationMinutes();
        int requiredBlocks = calculateRequiredBlocks(estimatedDurationMinutes, slotDurationMinutes);

        for (int i = 0; i < requiredBlocks; i++) {
            LocalTime blockTime = startTime.plusMinutes((long) i * slotDurationMinutes);

            if (!isWithinWorkingHours(date, blockTime, slotDurationMinutes)) {
                return false;
            }

            TimeSlot slotConfig = timeSlotRepository.findByStartTime(blockTime)
                    .orElse(null);
            if (slotConfig == null || Boolean.FALSE.equals(slotConfig.getIsActive())) {
                return false;
            }

            List<SlotReservation> reservations =
                    reservationRepository.findByDateAndTime(date, blockTime);

            int count = 0;
            for (SlotReservation r : reservations) {
                Integer bookingId = r.getBookingId();
                if (excludeBookingId != null && bookingId.equals(excludeBookingId)) {
                    continue;
                }
                count = count + 1;
            }

            if (count >= slotConfig.getCapacity()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Kiểm tra slot và reserve ngay trong cùng 1 transaction có pessimistic lock.
     * Dùng method này thay vì gọi isSlotAvailable() + reserveForBooking() riêng lẻ
     * để tránh race condition (overbooking).
     *
     * @param bookingId            ID booking vừa được save
     * @param date                 Ngày đặt
     * @param startTime            Giờ bắt đầu
     * @param durationMinutes      Thời lượng ước tính
     * @param excludeBookingId     Booking cần exclude khi check (dùng khi modify)
     * @throws com.g42.platform.gms.booking.customer.domain.exception.BookingException nếu slot đã đầy
     */
    @Transactional
    public void checkAndReserve(Integer bookingId, LocalDate date, LocalTime startTime,
                                int durationMinutes, Integer excludeBookingId) {
        int slotDurationMinutes = getSlotDurationMinutes();
        int requiredBlocks = calculateRequiredBlocks(durationMinutes, slotDurationMinutes);

        for (int i = 0; i < requiredBlocks; i++) {
            LocalTime blockTime = startTime.plusMinutes((long) i * slotDurationMinutes);

            if (!isWithinWorkingHours(date, blockTime, slotDurationMinutes)) {
                throw new com.g42.platform.gms.booking.customer.domain.exception.BookingException(
                        "Ngoài giờ hoạt động của xưởng: " + blockTime);
            }

            // Pessimistic lock trên time_slot row — chặn concurrent check cùng slot
            TimeSlot slotConfig = timeSlotRepository.findByStartTimeWithLock(blockTime)
                    .orElseThrow(() -> new com.g42.platform.gms.booking.customer.domain.exception.BookingException(
                            "Khung giờ không tồn tại: " + blockTime));

            if (Boolean.FALSE.equals(slotConfig.getIsActive())) {
                throw new com.g42.platform.gms.booking.customer.domain.exception.BookingException(
                        "Khung giờ không hoạt động: " + blockTime);
            }

            List<SlotReservation> reservations = reservationRepository.findByDateAndTime(date, blockTime);
            int count = 0;
            for (SlotReservation r : reservations) {
                if (excludeBookingId != null && excludeBookingId.equals(r.getBookingId())) {
                    continue;
                }
                count++;
            }

            if (count >= slotConfig.getCapacity()) {
                throw new com.g42.platform.gms.booking.customer.domain.exception.BookingException(
                        "Khung giờ này đã đầy, vui lòng chọn giờ khác.");
            }

            // Reserve ngay trong cùng transaction đang giữ lock
            SlotReservation reservation = new SlotReservation();
            reservation.setBookingId(bookingId);
            reservation.setReservedDate(date);
            reservation.setStartTime(blockTime);
            reservationRepository.save(reservation);
        }

        log.debug("checkAndReserve: reserved {} blocks for booking {}", requiredBlocks, bookingId);
    }
    @Transactional
    public void checkAndReserveForStaff(Integer bookingId, LocalDate date, LocalTime startTime,
                                int durationMinutes, Integer excludeBookingId) {
        int slotDurationMinutes = getSlotDurationMinutes();
        int requiredBlocks = calculateRequiredBlocks(durationMinutes, slotDurationMinutes);

        for (int i = 0; i < requiredBlocks; i++) {
            LocalTime blockTime = startTime.plusMinutes((long) i * slotDurationMinutes);

            // Pessimistic lock trên time_slot row — chặn concurrent check cùng slot
            TimeSlot slotConfig = timeSlotRepository.findByStartTimeWithLock(blockTime)
                    .orElseThrow(() -> new com.g42.platform.gms.booking.customer.domain.exception.BookingException(
                            "Khung giờ không tồn tại: " + blockTime));

            if (Boolean.FALSE.equals(slotConfig.getIsActive())) {
                throw new com.g42.platform.gms.booking.customer.domain.exception.BookingException(
                        "Khung giờ không hoạt động: " + blockTime);
            }

            List<SlotReservation> reservations = reservationRepository.findByDateAndTime(date, blockTime);
            int count = 0;
            for (SlotReservation r : reservations) {
                if (excludeBookingId != null && excludeBookingId.equals(r.getBookingId())) {
                    continue;
                }
                count++;
            }

            // Reserve ngay trong cùng transaction đang giữ lock
            SlotReservation reservation = new SlotReservation();
            reservation.setBookingId(bookingId);
            reservation.setReservedDate(date);
            reservation.setStartTime(blockTime);
            reservationRepository.save(reservation);
        }

        log.debug("checkAndReserve: reserved {} blocks for booking {}", requiredBlocks, bookingId);
    }

    /**
     * Reserve (đặt chỗ) slots cho một booking
     *
     * Tạo SlotReservation records cho tất cả blocks cần thiết
     *
     * @param bookingId ID của booking
     * @param estimatedDurationMinutes Thời lượng ước tính
     */
    @Transactional
    public void reserveForBooking(Integer bookingId, int estimatedDurationMinutes) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        int slotDurationMinutes = getSlotDurationMinutes();
        int blocks = calculateRequiredBlocks(estimatedDurationMinutes, slotDurationMinutes);
        LocalDate date = booking.getScheduledDate();
        LocalTime time = booking.getScheduledTime();

        for (int i = 0; i < blocks; i++) {
            LocalTime blockTime = time.plusMinutes((long) i * slotDurationMinutes);

            SlotReservation reservation = new SlotReservation();
            reservation.setBookingId(bookingId);
            reservation.setReservedDate(date);
            reservation.setStartTime(blockTime);

            reservationRepository.save(reservation);
        }

        log.debug("Reserved {} blocks for booking {}", blocks, bookingId);
    }

    /**
     * Release (giải phóng) slots của một booking
     * Xóa tất cả SlotReservation records của booking đó
     *
     * @param bookingId ID của booking
     */
    @Transactional
    public void releaseForBooking(Integer bookingId) {
        reservationRepository.deleteByBookingId(bookingId);
        log.debug("Released slots for booking {}", bookingId);
    }

    /**
     * Lấy danh sách slots available cho CUSTOMER
     *
     * Business Rule:
     * - Filter slots trong quá khứ
     * - Filter slots quá gần (< minLeadTimeHours từ hiện tại)
     * - Chỉ trả về slots còn trống
     *
     * @param date Ngày cần check
     * @param estimatedDurationMinutes Thời lượng ước tính
     * @return Danh sách slots available
     */
    public List<TimeSlotResponse> getAvailableSlotsForCustomer(
            LocalDate date, int estimatedDurationMinutes) {

        List<TimeSlotResponse> result = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minSlotTime = now.plusHours(getMinLeadTimeHours());

        List<TimeSlot> allSlots = timeSlotRepository.findActiveOrderByStartTime();

        for (TimeSlot slotConfig : allSlots) {
            LocalTime slotTime = slotConfig.getStartTime();
            LocalDateTime slotDateTime = LocalDateTime.of(date, slotTime);

            // Filter slot quá khứ hoặc quá gần
            if (slotDateTime.isBefore(minSlotTime)) {
                continue;
            }

            boolean available = isSlotAvailable(date, slotTime, estimatedDurationMinutes, null);

            if (available) {
                List<SlotReservation> reservations = reservationRepository.findByDateAndTime(date, slotTime);
                int count = reservations.size();
                int remainingCapacity = slotConfig.getCapacity() - count;

                TimeSlotResponse dto = new TimeSlotResponse();
                dto.setSlotId(slotConfig.getSlotId());
                dto.setStartTime(slotTime);
                dto.setPeriod(slotConfig.getPeriod());
                dto.setCapacity(slotConfig.getCapacity());
                dto.setIsActive(slotConfig.getIsActive());
                dto.setRemainingCapacity(remainingCapacity);
                dto.setIsAvailable(true);
                dto.setStatus("Còn trống");

                result.add(dto);
            }
        }

        return result;
    }
    /**
         * Lấy danh sách slots available cho STAFF/RECEPTIONIST
         *
         * Business Rule:
         * - Chỉ filter slots trong quá khứ
         * - KHÔNG filter lead-time (staff có thể đặt ngay tức khắc)
         * - Vẫn phải nằm trong giờ hoạt động của xưởng ngày đó
         * - Chỉ trả về slots còn trống
         *
         * @param date Ngày cần check
         * @param estimatedDurationMinutes Thời lượng ước tính
         * @return Danh sách slots available
         */
        public List<TimeSlotResponse> getAvailableSlotsForStaff(
                LocalDate date, int estimatedDurationMinutes) {

            List<TimeSlotResponse> result = new ArrayList<>();

            LocalDateTime now = LocalDateTime.now();
            int slotDurationMinutes = getSlotDurationMinutes();

            List<TimeSlot> allSlots = timeSlotRepository.findActiveOrderByStartTime();

            for (TimeSlot slotConfig : allSlots) {
                LocalTime slotTime = slotConfig.getStartTime();
                LocalDateTime slotDateTime = LocalDateTime.of(date, slotTime);

                // Staff chỉ cần check không được trong quá khứ (không check lead-time)
                if (slotDateTime.isBefore(now)) {
                    continue;
                }

                if (!isWithinWorkingHours(date, slotTime, slotDurationMinutes)) {
                    continue;
                }

                List<SlotReservation> reservations = reservationRepository.findByDateAndTime(date, slotTime);
                int count = reservations.size();
                int capacity = slotConfig.getCapacity();
                int remainingCapacity = capacity - count;
                boolean overCapacity = count >= capacity;

                TimeSlotResponse dto = new TimeSlotResponse();
                dto.setSlotId(slotConfig.getSlotId());
                dto.setStartTime(slotTime);
                dto.setPeriod(slotConfig.getPeriod());
                dto.setCapacity(capacity);
                dto.setIsActive(slotConfig.getIsActive());
                dto.setCurrentBookingCount(count);
                dto.setRemainingCapacity(Math.max(0, remainingCapacity));
                dto.setIsAvailable(!overCapacity);
                dto.setIsOverCapacity(overCapacity);
                dto.setStatus(overCapacity ? "Đã đầy (vượt capacity)" : "Còn trống");

                result.add(dto);
            }

            return result;
        }



    /**
     * Get all time slots (for frontend to display all available time options)
     */
    public List<TimeSlot> getAllTimeSlots() {
        log.debug("Fetching all time slots");
        return timeSlotRepository.findAllOrderByStartTime();
    }

    /**
     * Get only active time slots
     */
    public List<TimeSlot> getActiveTimeSlots() {
        log.debug("Fetching active time slots");
        return timeSlotRepository.findActiveOrderByStartTime();
    }

    /**
     * Get reservation count for a specific date and time
     */
    public int getReservationCount(LocalDate date, LocalTime startTime) {
        List<SlotReservation> reservations = reservationRepository.findByDateAndTime(date, startTime);
        return reservations.size();
    }

    /**
     * Độ dài 1 block slot (phút), đọc từ BookingConfig (mục Cấu hình hệ thống).
     * Fallback 30 phút nếu chưa có config trong DB.
     */
    private int getSlotDurationMinutes() {
        return bookingConfigJpaRepo.findById(1)
                .map(BookingConfigJpa::getSlotDurationMinutes)
                .orElse(30);
    }

    /**
     * Số giờ tối thiểu phải đặt trước, đọc từ BookingConfig (mục Cấu hình hệ thống).
     * Fallback 2 giờ nếu chưa có config trong DB.
     */
    private long getMinLeadTimeHours() {
        return bookingConfigJpaRepo.findById(1)
                .map(c -> c.getMinLeadTimeHours().longValue())
                .orElse(2L);
    }

    /**
     * Check block giờ [blockTime, blockTime + slotDurationMinutes) có nằm trong giờ hoạt động
     * của xưởng ngày `date` không (đúng ngày mở cửa, trong khoảng open-close, không rơi vào giờ nghỉ trưa).
     */
    private boolean isWithinWorkingHours(LocalDate date, LocalTime blockTime, int slotDurationMinutes) {
        int dayOfWeek = date.getDayOfWeek().getValue(); // 1 = Thứ 2 ... 7 = Chủ nhật
        WorkingHoursJpa wh = workingHoursJpaRepo.findByDayOfWeek(dayOfWeek).orElse(null);

        if (wh == null || Boolean.FALSE.equals(wh.getIsOpen())
                || wh.getOpenTime() == null || wh.getCloseTime() == null) {
            return false;
        }

        LocalTime blockEnd = blockTime.plusMinutes(slotDurationMinutes);
        if (blockTime.isBefore(wh.getOpenTime()) || blockEnd.isAfter(wh.getCloseTime())) {
            return false;
        }

        if (wh.getBreakStart() != null && wh.getBreakEnd() != null) {
            boolean overlapsBreak = blockTime.isBefore(wh.getBreakEnd()) && blockEnd.isAfter(wh.getBreakStart());
            if (overlapsBreak) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tính số blocks (slots) cần thiết dựa trên duration
     *
     * Ví dụ (slotDurationMinutes = 30):
     * - 30 phút => 1 block
     * - 60 phút => 2 blocks
     * - 90 phút => 3 blocks
     * - 45 phút => 2 blocks (làm tròn lên)
     *
     * @param durationMinutes Thời lượng (phút)
     * @param slotDurationMinutes Độ dài 1 block (phút)
     * @return Số blocks cần thiết
     */
    private int calculateRequiredBlocks(int durationMinutes, int slotDurationMinutes) {
        if (durationMinutes <= 0) {
            return 1;
        }
        int blocks = durationMinutes / slotDurationMinutes;
        if (durationMinutes % slotDurationMinutes != 0) {
            blocks = blocks + 1;
        }
        return blocks;
    }
}
