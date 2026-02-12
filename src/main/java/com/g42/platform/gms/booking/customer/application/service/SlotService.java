package com.g42.platform.gms.booking.customer.application.service;

import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking.customer.domain.entity.SlotReservation;
import com.g42.platform.gms.booking.customer.domain.entity.TimeSlot;
import com.g42.platform.gms.booking.customer.domain.repository.BookingRepository;
import com.g42.platform.gms.booking.customer.domain.repository.SlotReservationRepository;
import com.g42.platform.gms.booking.customer.domain.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotService {

    private static final int BASE_SLOT_MINUTES = 30;
    private static final long MIN_BOOKING_LEAD_TIME_HOURS = 2L;

    private final TimeSlotRepository timeSlotRepository;
    private final SlotReservationRepository reservationRepository;
    private final BookingRepository bookingRepository;

    public boolean isSlotAvailable(LocalDate date,
                                   LocalTime startTime,
                                   int estimatedDurationMinutes,
                                   Integer excludeBookingId) {

        int requiredBlocks = calculateRequiredBlocks(estimatedDurationMinutes);

        for (int i = 0; i < requiredBlocks; i++) {
            LocalTime blockTime = startTime.plusMinutes((long) i * BASE_SLOT_MINUTES);

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

    public void reserveForBooking(Integer bookingId, int estimatedDurationMinutes) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        
        int blocks = calculateRequiredBlocks(estimatedDurationMinutes);
        LocalDate date = booking.getScheduledDate();
        LocalTime time = booking.getScheduledTime();

        for (int i = 0; i < blocks; i++) {
            LocalTime blockTime = time.plusMinutes((long) i * BASE_SLOT_MINUTES);

            SlotReservation reservation = new SlotReservation();
            reservation.setBookingId(bookingId);
            reservation.setReservedDate(date);
            reservation.setStartTime(blockTime);

            reservationRepository.save(reservation);
        }

        log.debug("Reserved {} blocks for booking {}", blocks, bookingId);
    }

    public void releaseForBooking(Integer bookingId) {
        reservationRepository.deleteByBookingId(bookingId);
        log.debug("Released slots for booking {}", bookingId);
    }

    public List<com.g42.platform.gms.booking.customer.api.dto.AvailableSlotResponse> getAvailableSlotsForCustomer(
            LocalDate date, int estimatedDurationMinutes) {
        
        List<com.g42.platform.gms.booking.customer.api.dto.AvailableSlotResponse> result = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minSlotTime = now.plusHours(MIN_BOOKING_LEAD_TIME_HOURS);
        
        List<TimeSlot> allSlots = timeSlotRepository.findActiveOrderByStartTime();
        
        for (TimeSlot slotConfig : allSlots) {
            LocalTime slotTime = slotConfig.getStartTime();
            LocalDateTime slotDateTime = LocalDateTime.of(date, slotTime);
            
            // Filter slot quá khứ hoặc quá gần (< 2h)
            if (slotDateTime.isBefore(minSlotTime)) {
                continue;
            }
            
            boolean available = isSlotAvailable(date, slotTime, estimatedDurationMinutes, null);
            
            if (available) {
                List<SlotReservation> reservations =
                        reservationRepository.findByDateAndTime(date, slotTime);
                int count = reservations.size();
                int remainingCapacity = slotConfig.getCapacity() - count;
                
                com.g42.platform.gms.booking.customer.api.dto.AvailableSlotResponse dto =
                        new com.g42.platform.gms.booking.customer.api.dto.AvailableSlotResponse();
                dto.setStartTime(slotTime);
                dto.setPeriod(slotConfig.getPeriod());
                dto.setRemainingCapacity(remainingCapacity);
                dto.setIsAvailable(true);
                dto.setStatus("Còn trống");
                
                result.add(dto);
            }
        }
        
        return result;
    }

    private int calculateRequiredBlocks(int durationMinutes) {
        if (durationMinutes <= 0) {
            return 1;
        }
        int blocks = durationMinutes / BASE_SLOT_MINUTES;
        if (durationMinutes % BASE_SLOT_MINUTES != 0) {
            blocks = blocks + 1;
        }
        return blocks;
    }
}
