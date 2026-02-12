package com.g42.platform.gms.booking.customer.infrastructure.implement;

import com.g42.platform.gms.booking.customer.domain.entity.SlotReservation;
import com.g42.platform.gms.booking.customer.domain.repository.SlotReservationRepository;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingSlotReservationJpaEntity;
import com.g42.platform.gms.booking.customer.infrastructure.mapper.SlotReservationMapper;
import com.g42.platform.gms.booking.customer.infrastructure.repository.BookingJpaRepository;
import com.g42.platform.gms.booking.customer.infrastructure.repository.BookingSlotReservationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SlotReservationRepositoryImpl implements SlotReservationRepository {

    private final BookingSlotReservationJpaRepository jpaRepository;
    private final BookingJpaRepository bookingJpaRepository;
    private final SlotReservationMapper mapper;

    @Override
    public List<SlotReservation> findByDateAndTime(LocalDate date, LocalTime time) {
        return jpaRepository.findByReservedDateAndStartTime(date, time)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SlotReservation> findByBookingId(Integer bookingId) {
        return jpaRepository.findByBooking_BookingId(bookingId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByBookingId(Integer bookingId) {
        jpaRepository.deleteByBooking_BookingId(bookingId);
    }

    @Override
    public SlotReservation save(SlotReservation domain) {
        BookingSlotReservationJpaEntity jpa = mapper.toJpa(domain);
        
        if (domain.getBookingId() != null) {
            BookingJpaEntity booking = bookingJpaRepository.findById(domain.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + domain.getBookingId()));
            jpa.setBooking(booking);
        }
        
        BookingSlotReservationJpaEntity saved = jpaRepository.save(jpa);
        return mapper.toDomain(saved);
    }
}
