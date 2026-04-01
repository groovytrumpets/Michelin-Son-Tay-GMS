package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceWriterTest {

    @Mock
    private AttendanceCheckinJpaRepo checkinRepo;

    @InjectMocks
    private AttendanceWriter writer;

    private WorkShiftJpa morningShift;

    @BeforeEach
    void setUp() {
        morningShift = new WorkShiftJpa();
        morningShift.setShiftId(1);
        morningShift.setShiftName("Ca Sáng");
        morningShift.setStartTime(LocalTime.of(7, 0));
        morningShift.setEndTime(LocalTime.of(12, 0));
        morningShift.setIsActive(true);
    }

    @Test
    void noExistingRecord_shouldCreateCheckIn_withPresentStatus() {
        // Quét lúc 06:55 (trước giờ ca) → PRESENT
        LocalDateTime scanTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(6, 55));
        when(checkinRepo.findByStaffIdAndAttendanceDateAndShiftId(1, scanTime.toLocalDate(), 1))
                .thenReturn(Optional.empty());
        when(checkinRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writer.write(1, 1, scanTime, morningShift);

        ArgumentCaptor<AttendanceCheckinJpa> captor = ArgumentCaptor.forClass(AttendanceCheckinJpa.class);
        verify(checkinRepo).save(captor.capture());
        AttendanceCheckinJpa saved = captor.getValue();

        assertThat(saved.getCheckInTime()).isEqualTo(LocalTime.of(6, 55));
        assertThat(saved.getStatus()).isEqualTo("PRESENT");
        assertThat(saved.getNotes()).isEqualTo("Auto sync from Hikvision");
        assertThat(saved.getCheckOutTime()).isNull();
    }

    @Test
    void noExistingRecord_scanAfterStartTime_shouldCreateCheckIn_withLateStatus() {
        // Quét lúc 07:30 (sau giờ ca) → LATE
        LocalDateTime scanTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 30));
        when(checkinRepo.findByStaffIdAndAttendanceDateAndShiftId(1, scanTime.toLocalDate(), 1))
                .thenReturn(Optional.empty());
        when(checkinRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writer.write(1, 1, scanTime, morningShift);

        ArgumentCaptor<AttendanceCheckinJpa> captor = ArgumentCaptor.forClass(AttendanceCheckinJpa.class);
        verify(checkinRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("LATE");
    }

    @Test
    void existingCheckIn_scanAfter2Hours_shouldUpdateCheckOut() {
        // Đã check-in lúc 07:00, quét lại lúc 11:30 (cách 4.5h) → update check-out
        LocalDateTime scanTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 30));

        AttendanceCheckinJpa existing = new AttendanceCheckinJpa();
        existing.setCheckinId(10);
        existing.setStaffId(1);
        existing.setShiftId(1);
        existing.setCheckInTime(LocalTime.of(7, 0));
        existing.setCheckOutTime(null);
        existing.setStatus("PRESENT");

        when(checkinRepo.findByStaffIdAndAttendanceDateAndShiftId(1, scanTime.toLocalDate(), 1))
                .thenReturn(Optional.of(existing));
        when(checkinRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writer.write(1, 1, scanTime, morningShift);

        ArgumentCaptor<AttendanceCheckinJpa> captor = ArgumentCaptor.forClass(AttendanceCheckinJpa.class);
        verify(checkinRepo).save(captor.capture());
        assertThat(captor.getValue().getCheckOutTime()).isEqualTo(LocalTime.of(11, 30));
        // Status không thay đổi
        assertThat(captor.getValue().getStatus()).isEqualTo("PRESENT");
    }

    @Test
    void existingCheckIn_scanWithin2Hours_shouldSkipCheckOut() {
        // Đã check-in lúc 07:00, quét lại lúc 08:30 (chỉ 1.5h) → bỏ qua
        LocalDateTime scanTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 30));

        AttendanceCheckinJpa existing = new AttendanceCheckinJpa();
        existing.setCheckInTime(LocalTime.of(7, 0));
        existing.setCheckOutTime(null);

        when(checkinRepo.findByStaffIdAndAttendanceDateAndShiftId(1, scanTime.toLocalDate(), 1))
                .thenReturn(Optional.of(existing));

        writer.write(1, 1, scanTime, morningShift);

        // Không gọi save
        verify(checkinRepo, never()).save(any());
    }

    @Test
    void existingFullRecord_shouldSkipCompletely() {
        // Đã có cả check-in và check-out → bỏ qua hoàn toàn
        LocalDateTime scanTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 0));

        AttendanceCheckinJpa existing = new AttendanceCheckinJpa();
        existing.setCheckInTime(LocalTime.of(7, 0));
        existing.setCheckOutTime(LocalTime.of(12, 0));

        when(checkinRepo.findByStaffIdAndAttendanceDateAndShiftId(1, scanTime.toLocalDate(), 1))
                .thenReturn(Optional.of(existing));

        writer.write(1, 1, scanTime, morningShift);

        verify(checkinRepo, never()).save(any());
    }
}
