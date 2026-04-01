package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftResolverTest {

    private ShiftResolver resolver;

    private WorkShiftJpa morningShift;  // 07:00 - 12:00
    private WorkShiftJpa afternoonShift; // 13:00 - 18:00

    @BeforeEach
    void setUp() {
        resolver = new ShiftResolver();

        morningShift = new WorkShiftJpa();
        morningShift.setShiftId(1);
        morningShift.setShiftName("Ca Sáng");
        morningShift.setStartTime(LocalTime.of(7, 0));
        morningShift.setEndTime(LocalTime.of(12, 0));
        morningShift.setIsActive(true);

        afternoonShift = new WorkShiftJpa();
        afternoonShift.setShiftId(2);
        afternoonShift.setShiftName("Ca Chiều");
        afternoonShift.setStartTime(LocalTime.of(13, 0));
        afternoonShift.setEndTime(LocalTime.of(18, 0));
        afternoonShift.setIsActive(true);
    }

    @Test
    void scanAtExactStartTime_shouldResolveMorningShift() {
        // Quét đúng 07:00 → Ca Sáng
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(7, 0), List.of(morningShift, afternoonShift));
        assertThat(result).isPresent();
        assertThat(result.get().getShiftId()).isEqualTo(1);
    }

    @Test
    void scanEarly30Minutes_shouldResolveMorningShift() {
        // Quét lúc 06:30 (sớm đúng 30 phút) → Ca Sáng (Step 1)
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(6, 30), List.of(morningShift, afternoonShift));
        assertThat(result).isPresent();
        assertThat(result.get().getShiftId()).isEqualTo(1);
    }

    @Test
    void scanAfterStartTime_shouldResolveMorningShift() {
        // Quét lúc 07:15 (trễ 15 phút) → Ca Sáng
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(7, 15), List.of(morningShift, afternoonShift));
        assertThat(result).isPresent();
        assertThat(result.get().getShiftId()).isEqualTo(1);
    }

    @Test
    void scanAtAfternoonStart_shouldResolveAfternoonShift() {
        // Quét lúc 13:05 → Ca Chiều
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(13, 5), List.of(morningShift, afternoonShift));
        assertThat(result).isPresent();
        assertThat(result.get().getShiftId()).isEqualTo(2);
    }

    @Test
    void scanEarlyAfternoon_shouldResolveAfternoonShift() {
        // Quét lúc 12:35 (sớm 25 phút trước Ca Chiều) → Ca Chiều (Step 1)
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(12, 35), List.of(morningShift, afternoonShift));
        assertThat(result).isPresent();
        assertThat(result.get().getShiftId()).isEqualTo(2);
    }

    @Test
    void scanTooEarly_outsideAllShifts_shouldReturnEmpty() {
        // Quét lúc 03:00 (ngoài ±2h so với tất cả ca) → empty
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(3, 0), List.of(morningShift, afternoonShift));
        assertThat(result).isEmpty();
    }

    @Test
    void emptyShiftList_shouldReturnEmpty() {
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(7, 0), List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void inactiveShift_shouldBeIgnored() {
        // Ca Sáng bị inactive → chỉ còn Ca Chiều
        morningShift.setIsActive(false);
        // Nhưng ShiftResolver nhận activeShifts đã được filter trước, nên test với list chỉ có Ca Chiều
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(7, 0), List.of(afternoonShift));
        // 07:00 cách 13:00 là 6 tiếng, ngoài ±2h → empty
        assertThat(result).isEmpty();
    }

    @Test
    void scanBetweenShifts_shouldResolveFallbackToClosest() {
        // Quét lúc 12:00 — thỏa Step 1 với Ca Sáng (12:00 >= 07:00 - 30min = 06:30)
        // Kết quả: Ca Sáng (gần nhất thỏa điều kiện Step 1)
        Optional<WorkShiftJpa> result = resolver.resolve(LocalTime.of(12, 0), List.of(morningShift, afternoonShift));
        assertThat(result).isPresent();
        assertThat(result.get().getShiftId()).isEqualTo(1); // Ca Sáng
    }
}
