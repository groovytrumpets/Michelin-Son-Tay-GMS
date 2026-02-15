package com.g42.platform.gms.booking_management.infrastructure;

import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequest;
import com.g42.platform.gms.booking_management.domain.repository.BookingManageRepository;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import com.g42.platform.gms.booking_management.infrastructure.mapper.BookingDetailManagerMapper;
import com.g42.platform.gms.booking_management.infrastructure.mapper.BookingDraffManagerMapper;
import com.g42.platform.gms.booking_management.infrastructure.mapper.BookingManagerMapper;
import com.g42.platform.gms.booking_management.infrastructure.repository.BookingMDetailJpaRepo;
import com.g42.platform.gms.booking_management.infrastructure.repository.BookingMRequestJpaRepo;
import com.g42.platform.gms.booking_management.infrastructure.repository.BookingManageJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
