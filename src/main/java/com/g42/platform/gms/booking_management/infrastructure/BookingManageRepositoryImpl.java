package com.g42.platform.gms.booking_management.infrastructure;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.domain.entity.*;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.domain.repository.BookingManageRepository;
import com.g42.platform.gms.booking_management.infrastructure.entity.*;
import com.g42.platform.gms.booking_management.infrastructure.mapper.*;
import com.g42.platform.gms.booking_management.infrastructure.repository.*;
import com.g42.platform.gms.booking_management.infrastructure.specification.BookingRequestSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final CatalogItemManageMapper catalogItemManageMapper;
    @Override
    public Page<BookedRespond> getBookedList(int page, int size, LocalDate date, Boolean isGuest, BookingEnum status, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<BookingJpa> specification = Specification.unrestricted();
        specification = specification.and(BookingRequestSpecification.filterBooking(date,isGuest,status));
        if (search != null && !search.isBlank()) {
        specification = specification.and(BookingRequestSpecification.searchBooking(search));
        }

        Page<BookedRespond> bookingResponses = bookingManageJpaRepository.findAllBooked(specification,pageable);
        return bookingResponses;
    }

    @Override
    public Booking getBookedDetailById(Integer bookingId) {
        BookingJpa bookingJpa = bookingManageJpaRepository.getBookingJpaByBookingId(bookingId);
        return bookingManagerMapper.toDomain(bookingJpa);

    }
    @Override
    public Page<BookingRequest> getBookingRequestList(int page, int size, LocalDate date, Boolean isGuest, BookingRequestStatus status, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<BookingRequestJpa> specification = Specification.unrestricted();
        specification = specification.and(BookingRequestSpecification.filter(date,isGuest,status));
        if (search != null && !search.isBlank()) {
            specification = specification.and(BookingRequestSpecification.searchBookingRequest(search));
        }
        Page<BookingRequestJpa> bookingRequestJpaList = bookingMRequestJpaRepo.findAll(specification, pageable);
        return bookingRequestJpaList.map(bookingDraffManagerMapper::toDomain);
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
    public BookingJpa createBookingByRequest(BookingRequest request, int customerId) {
        //todo: fixing architect, not call customer another module repo
        Booking booking = new Booking();
        booking.setCreatedAt(LocalDateTime.now());
        booking.setBookingCode(request.getRequestCode());
        System.out.println("booking code: " + booking.getBookingCode());
        System.out.println("booking request code: " + request.getRequestCode());

        booking.setCustomerId(customerId);
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
