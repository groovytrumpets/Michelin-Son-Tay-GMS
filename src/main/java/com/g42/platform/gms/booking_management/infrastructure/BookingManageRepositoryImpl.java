package com.g42.platform.gms.booking_management.infrastructure;

import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking_management.domain.entity.*;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.domain.repository.BookingManageRepository;
import com.g42.platform.gms.booking_management.infrastructure.entity.*;
import com.g42.platform.gms.booking_management.infrastructure.mapper.*;
import com.g42.platform.gms.booking_management.infrastructure.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private final CatalogItemManageMapper catalogItemManageMapper;
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

       private final CustomerProfileRepository customerProfileRepository;
    @Override
    public BookingJpa createBookingByRequest(BookingRequest request) {
        //todo: fixing architect, not call customer another module repo
        Booking booking = new Booking();
        booking.setCreatedAt(LocalDateTime.now());
        booking.setCustomer(null);
        booking.setDescription(request.getDescription());
        booking.setIsGuest(request.getIsGuest());
        booking.setStatus(BookingEnum.CONFIRMED);
        booking.setServices(request.getServices());
        booking.setScheduledDate(request.getScheduledDate());
        booking.setScheduledTime(request.getScheduledTime());
        booking.setServiceCategory(request.getServiceCategory());
        BookingJpa bookingJpa = bookingManageJpaRepository.save(bookingManagerMapper.toBooking(booking));
        return bookingJpa;
    }
    @Override
    public BookingSlotReservation createBookingSlotReservation(BookingRequest request, BookingJpa bookingId) {
        //todo: fixing architect
        BookingSlotReservation bookingSlotReservation = new BookingSlotReservation();
        bookingSlotReservation.setBooking(bookingId);
        bookingSlotReservation.setStartTime(request.getScheduledTime());
        bookingSlotReservation.setReservedDate(request.getScheduledDate());
        BookingSlotReservationJpa bookingSlotReservationJpa = bookingMSlotReservationRepo.save(bookingMSlotReservationMapper.toJpa(bookingSlotReservation));
        return bookingMSlotReservationMapper.toDomain(bookingSlotReservationJpa);
    }

    @Override
    public void setConfirmStatus(BookingRequest request) {
        bookingMRequestJpaRepo.save(bookingDraffManagerMapper.toDomainJpa(request));
    }

    private final CatalogRepo catalogRepo;
    @Override
    public void setRequestBooking(BookingRequest request) {
        BookingRequestJpa bookingRequestJpa = bookingDraffManagerMapper.toDomainJpa(request);
        bookingMRequestJpaRepo.save(bookingRequestJpa);
    }

    @Override
    public List<CatalogItem> getListOfCatalogById(List<Integer> services) {
        List<CatalogItemJpa> catalogItems = catalogRepo.findAllById(services);
        return catalogItemManageMapper.getListOfCatalogItem(catalogItems);
    }
}
