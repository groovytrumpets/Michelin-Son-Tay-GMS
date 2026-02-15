package com.g42.platform.gms.booking_management.infrastructure;

import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequest;
import com.g42.platform.gms.booking_management.domain.entity.TimeSlot;
import com.g42.platform.gms.booking_management.domain.repository.BookingManageRepository;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingSlotReservationJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.TimeSlotJpa;
import com.g42.platform.gms.booking_management.infrastructure.mapper.*;
import com.g42.platform.gms.booking_management.infrastructure.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
@Repository
@AllArgsConstructor
public class BookingManageRepositoryImpl implements BookingManageRepository {
    private final BookingManageJpaRepository bookingManageJpaRepository;
    private final BookingMDetailJpaRepo bookingMDetailJpaRepo;
    private final BookingManagerMapper bookingManagerMapper;
    private final BookingDetailManagerMapper bookingDetailManagerMapper;
    private final BookingMRequestJpaRepo bookingMRequestJpaRepo;
    private final BookingDraffManagerMapper bookingDraffManagerMapper;
    @Override
    public List<Booking> getBookedList() {
        List<BookingJpa> bookingJpaList = bookingManageJpaRepository.findAll();
        return bookingManagerMapper.toBookingJpa(bookingJpaList);
    }

    @Override
    public Booking getBookedDetailById(Integer bookingId) {
        BookingJpa bookingJpa = bookingManageJpaRepository.getBookingJpaByBookingId(bookingId);
        return bookingManagerMapper.toDomain(bookingJpa);

    }

    @Override
    public List<BookingRequest> getBookingRequestList() {
        List<BookingRequestJpa> bookingRequestJpaList = bookingMRequestJpaRepo.findAll();
        return bookingDraffManagerMapper.toDomain(bookingRequestJpaList);
    }

    @Override
    public BookingRequest getBookingRequestById(Integer bookingId) {
        BookingRequestJpa bookingRequestJpa = bookingMRequestJpaRepo.searchBookingRequestJpaByRequestId(bookingId);
        return bookingDraffManagerMapper.toDomain(bookingRequestJpa);
    }

    private final TimeSlotJpaRepo timeSlotJpaRepo;
    private final TimeSlotMMapper timeSlotMMapper;
    @Override
    public TimeSlot getTimeSlotByTime(LocalTime scheduledTime) {
        TimeSlotJpa timeSlot = timeSlotJpaRepo.getTimeSlotsByStartTime(scheduledTime);
        return timeSlotMMapper.toDomainTimeSlot(timeSlot);
    }
    private final BookingMSlotReservationRepo bookingMSlotReservationRepo;
    private final BookingMSlotReservationMapper bookingMSlotReservationMapper;
    @Override
    public int countReserverdBasedOnTime(LocalTime scheduledTime) {
        List<BookingSlotReservationJpa> reservationJpaList = bookingMSlotReservationRepo.findAllByStartTime(scheduledTime);
        return reservationJpaList.size();
    }
}
