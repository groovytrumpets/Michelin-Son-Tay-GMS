package com.g42.platform.gms.hikvision.sync;

import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import com.g42.platform.gms.manager.attendance.infrastructure.repository.AttendanceCheckinJpaRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceWriterTest {

    @Mock
    private AttendanceCheckinJpaRepo checkinRepo;

    @InjectMocks
    private AttendanceWriter writer;

    @Test
    void noExistingRecord_shouldCreateCheckIn() {
        LocalDateTime scanTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 33));
        when(checkinRepo.findByStaffIdAndAttendanceDate(1, scanTime.toLocalDate()))
                .thenReturn(Collections.emptyList());
        when(checkinRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writer.write(1, scanTime);

        ArgumentCaptor<AttendanceCheckinJpa> captor = ArgumentCaptor.forClass(AttendanceCheckinJpa.class);
        verify(checkinRepo).save(captor.capture());
        AttendanceCheckinJpa saved = captor.getValue();

        assertThat(saved.getCheckInTime()).isEqualTo(LocalTime.of(7, 33));
        assertThat(saved.getCheckOutTime()).isNull();
        assertThat(saved.getNotes()).isEqualTo("Auto sync from Hikvision");
        assertThat(saved.getStatus()).isEqualTo("PRESENT");
    }

    @Test
    void existingCheckIn_laterScan_shouldUpdateCheckOut() {
        LocalDateTime scanTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(17, 0));

        AttendanceCheckinJpa existing = new AttendanceCheckinJpa();
        existing.setCheckinId(10);
        existing.setStaffId(1);
        existing.setCheckInTime(LocalTime.of(7, 33));
        existing.setCheckOutTime(null);
        existing.setStatus("PRESENT");

        when(checkinRepo.findByStaffIdAndAttendanceDate(1, scanTime.toLocalDate()))
                .thenReturn(List.of(existing));
        when(checkinRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        writer.write(1, scanTime);

        ArgumentCaptor<AttendanceCheckinJpa> captor = ArgumentCaptor.forClass(AttendanceCheckinJpa.class);
        verify(checkinRepo).save(captor.capture());
        assertThat(captor.getValue().getCheckOutTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void existingCheckIn_earlierScan_shouldNotUpdateCheckOut() {
        // Scan time trước check-in → bỏ qua (không thể xảy ra trong thực tế nhưng test phòng thủ)
        LocalDateTime scanTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(6, 0));

        AttendanceCheckinJpa existing = new AttendanceCheckinJpa();
        existing.setCheckInTime(LocalTime.of(7, 33));

        when(checkinRepo.findByStaffIdAndAttendanceDate(1, scanTime.toLocalDate()))
                .thenReturn(List.of(existing));

        writer.write(1, scanTime);

        verify(checkinRepo, never()).save(any());
    }
}
