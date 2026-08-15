package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.booking.customer.api.dto.BookingConfigDto;
import com.g42.platform.gms.booking.customer.api.dto.BookingScheduleConfigResponse;
import com.g42.platform.gms.booking.customer.api.dto.WorkingHoursDto;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingConfigJpa;
import com.g42.platform.gms.booking.customer.infrastructure.entity.TimeSlotJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.entity.WorkingHoursJpa;
import com.g42.platform.gms.booking.customer.infrastructure.repository.BookingConfigJpaRepo;
import com.g42.platform.gms.booking.customer.infrastructure.repository.TimeSlotJpaRepository;
import com.g42.platform.gms.booking.customer.infrastructure.repository.WorkingHoursJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Cấu hình lịch làm việc của xưởng (giờ mở/đóng cửa theo từng ngày trong tuần, nghỉ trưa,
 * độ dài slot, thời gian đặt tối thiểu, số ngày cho đặt trước, sức chứa mặc định).
 *
 * Khi lưu cấu hình, danh sách time_slot (khung giờ) được đồng bộ lại theo giờ hoạt động mới:
 * - Các mốc giờ không còn nằm trong giờ hoạt động của bất kỳ ngày nào sẽ bị vô hiệu hoá (is_active = false)
 *   thay vì xoá cứng, để không phá vỡ các slot đã có booking tham chiếu tới theo (ngày, giờ).
 * - Các mốc giờ mới sẽ được tạo, các mốc còn lại được cập nhật capacity theo defaultCapacity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingScheduleConfigService {

    private static final int CONFIG_ID = 1;

    private final BookingConfigJpaRepo bookingConfigJpaRepo;
    private final WorkingHoursJpaRepo workingHoursJpaRepo;
    private final TimeSlotJpaRepository timeSlotJpaRepository;

    public BookingScheduleConfigResponse getConfig() {
        BookingConfigJpa config = bookingConfigJpaRepo.findById(CONFIG_ID).orElseGet(this::newDefaultConfig);
        List<WorkingHoursJpa> hours = workingHoursJpaRepo.findAllByOrderByDayOfWeekAsc();
        if (hours.isEmpty()) {
            hours = defaultWorkingHours();
        }
        return toResponse(config, hours);
    }

    @Transactional
    public BookingScheduleConfigResponse updateConfig(BookingScheduleConfigResponse dto) {
        BookingConfigDto configDto = dto.getBookingConfig();

        BookingConfigJpa config = bookingConfigJpaRepo.findById(CONFIG_ID).orElseGet(this::newDefaultConfig);
        config.setSlotDurationMinutes(configDto.getSlotDurationMinutes());
        config.setMinLeadTimeHours(configDto.getMinLeadTimeHours());
        config.setDaysAheadHorizon(configDto.getDaysAheadHorizon());
        config.setDefaultCapacity(configDto.getDefaultCapacity());
        config.setUpdatedAt(LocalDateTime.now());
        bookingConfigJpaRepo.save(config);

        List<WorkingHoursJpa> existingHours = workingHoursJpaRepo.findAllByOrderByDayOfWeekAsc();
        Map<Integer, WorkingHoursJpa> byDay = existingHours.stream()
                .collect(Collectors.toMap(WorkingHoursJpa::getDayOfWeek, h -> h, (a, b) -> a));

        List<WorkingHoursJpa> saved = new ArrayList<>();
        for (WorkingHoursDto whDto : dto.getWorkingHours()) {
            WorkingHoursJpa wh = byDay.get(whDto.getDayOfWeek());
            if (wh == null) {
                wh = new WorkingHoursJpa();
                wh.setDayOfWeek(whDto.getDayOfWeek());
            }
            wh.setIsOpen(whDto.getIsOpen());
            wh.setOpenTime(whDto.getOpenTime());
            wh.setCloseTime(whDto.getCloseTime());
            wh.setBreakStart(whDto.getBreakStart());
            wh.setBreakEnd(whDto.getBreakEnd());
            wh.setUpdatedAt(LocalDateTime.now());
            saved.add(wh);
        }
        saved = workingHoursJpaRepo.saveAll(saved);

        regenerateTimeSlots(config, saved);

        return toResponse(config, saved);
    }

    private void regenerateTimeSlots(BookingConfigJpa config, List<WorkingHoursJpa> workingHours) {
        int duration = config.getSlotDurationMinutes();
        int defaultCapacity = config.getDefaultCapacity();

        Set<LocalTime> targetTimes = new TreeSet<>();
        for (WorkingHoursJpa wh : workingHours) {
            if (!Boolean.TRUE.equals(wh.getIsOpen()) || wh.getOpenTime() == null || wh.getCloseTime() == null) {
                continue;
            }
            LocalTime t = wh.getOpenTime();
            while (!t.plusMinutes(duration).isAfter(wh.getCloseTime())) {
                boolean inBreak = wh.getBreakStart() != null && wh.getBreakEnd() != null
                        && t.isBefore(wh.getBreakEnd()) && t.plusMinutes(duration).isAfter(wh.getBreakStart());
                if (!inBreak) {
                    targetTimes.add(t);
                }
                t = t.plusMinutes(duration);
            }
        }

        List<TimeSlotJpaEntity> existingSlots = timeSlotJpaRepository.findAll();
        Map<LocalTime, TimeSlotJpaEntity> existingByTime = existingSlots.stream()
                .collect(Collectors.toMap(TimeSlotJpaEntity::getStartTime, s -> s, (a, b) -> a));

        // Vô hiệu hoá các mốc giờ không còn nằm trong giờ hoạt động mới (không xoá cứng)
        for (TimeSlotJpaEntity existing : existingSlots) {
            if (!targetTimes.contains(existing.getStartTime()) && Boolean.TRUE.equals(existing.getIsActive())) {
                existing.setIsActive(false);
                timeSlotJpaRepository.save(existing);
            }
        }

        // Tạo mới / cập nhật capacity cho các mốc giờ thuộc giờ hoạt động hiện tại
        for (LocalTime time : targetTimes) {
            TimeSlotJpaEntity slot = existingByTime.get(time);
            if (slot == null) {
                slot = new TimeSlotJpaEntity();
                slot.setStartTime(time);
                slot.setPeriod(derivePeriod(time));
            }
            slot.setCapacity(defaultCapacity);
            slot.setIsActive(true);
            timeSlotJpaRepository.save(slot);
        }

        log.info("Regenerated time_slot template: {} active start times (duration={}min, capacity={})",
                targetTimes.size(), duration, defaultCapacity);
    }

    private String derivePeriod(LocalTime time) {
        if (time.isBefore(LocalTime.NOON)) {
            return "sáng";
        }
        if (time.isBefore(LocalTime.of(18, 0))) {
            return "chiều";
        }
        return "tối";
    }

    private BookingConfigJpa newDefaultConfig() {
        BookingConfigJpa c = new BookingConfigJpa();
        c.setId(CONFIG_ID);
        return c;
    }

    private List<WorkingHoursJpa> defaultWorkingHours() {
        List<WorkingHoursJpa> list = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            WorkingHoursJpa wh = new WorkingHoursJpa();
            wh.setDayOfWeek(day);
            if (day == 7) {
                wh.setIsOpen(false);
            } else if (day == 6) {
                wh.setIsOpen(true);
                wh.setOpenTime(LocalTime.of(8, 0));
                wh.setCloseTime(LocalTime.of(12, 0));
            } else {
                wh.setIsOpen(true);
                wh.setOpenTime(LocalTime.of(8, 0));
                wh.setCloseTime(LocalTime.of(17, 0));
                wh.setBreakStart(LocalTime.of(12, 0));
                wh.setBreakEnd(LocalTime.of(13, 0));
            }
            list.add(wh);
        }
        return list;
    }

    private BookingScheduleConfigResponse toResponse(BookingConfigJpa config, List<WorkingHoursJpa> hours) {
        BookingConfigDto configDto = new BookingConfigDto(
                config.getSlotDurationMinutes(),
                config.getMinLeadTimeHours(),
                config.getDaysAheadHorizon(),
                config.getDefaultCapacity()
        );
        List<WorkingHoursDto> hoursDto = hours.stream()
                .map(h -> new WorkingHoursDto(
                        h.getDayOfWeek(), h.getIsOpen(), h.getOpenTime(), h.getCloseTime(),
                        h.getBreakStart(), h.getBreakEnd()))
                .collect(Collectors.toList());
        return new BookingScheduleConfigResponse(configDto, hoursDto);
    }
}
