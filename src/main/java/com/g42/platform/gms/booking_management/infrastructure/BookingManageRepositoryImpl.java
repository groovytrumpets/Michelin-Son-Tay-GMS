package com.g42.platform.gms.booking_management.infrastructure;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.api.dto.CustomerDto;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.api.dto.requesting.QueueOrderItem;
import com.g42.platform.gms.booking_management.api.dto.requesting.ReorderQueueRequest;
import com.g42.platform.gms.booking_management.domain.entity.*;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.domain.repository.BookingManageRepository;
import com.g42.platform.gms.booking_management.infrastructure.entity.*;
import com.g42.platform.gms.booking_management.infrastructure.mapper.*;
import com.g42.platform.gms.booking_management.infrastructure.repository.*;
import com.g42.platform.gms.booking_management.infrastructure.specification.BookingRequestSpecification;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerProfileJpa;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerProfileJpaRepo;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private final CustomerProfileJpaRepo customerProfileJpaRepo;

    @Override
    public Page<BookedRespond> getBookedList(int page, int size, LocalDate date, Boolean isGuest, BookingEnum status, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<BookingJpa> specification = Specification.unrestricted();
        specification = specification.and(BookingRequestSpecification.filterBooking(date, isGuest, status));
        if (search != null && !search.isBlank()) {
            specification = specification.and(BookingRequestSpecification.searchBooking(search));
        }

        Page<BookingJpa> bookingPage = bookingManageJpaRepository.findAll(specification, pageable);

        // Batch query customers
        List<Integer> customerIds = bookingPage.getContent().stream()
                .map(BookingJpa::getCustomerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Integer, CustomerProfileJpa> customerMap = customerProfileJpaRepo
                .findAllById(customerIds).stream()
                .collect(Collectors.toMap(CustomerProfileJpa::getCustomerId, c -> c));

        return bookingPage.map(b -> {
            CustomerProfileJpa customer = customerMap.get(b.getCustomerId());
            return new BookedRespond(
                    b.getBookingId(),
                    b.getBookingCode(),
                    new CustomerDto(
                            customer != null ? customer.getCustomerId() : null,
                            customer != null ? customer.getFullName() : null,
                            customer != null ? customer.getPhone() : null,
                            null
                    ),
                    b.getScheduledDate(),
                    b.getScheduledTime(),
                    b.getStatus(),
                    b.getDescription(),
                    b.getIsGuest(),
                    b.getCreatedAt(),
                    b.getEstimateTime(),
                    b.getQueueOrder()
            );
        });
    }

    @Override
    public Booking getBookedDetailById(String bookingId) {
        BookingJpa bookingJpa = bookingManageJpaRepository.getBookingJpaByBookingCode(bookingId);
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
    public BookingRequest getBookingRequestById(String bookingId) {
        BookingRequestJpa bookingRequestJpa = bookingMRequestJpaRepo.searchBookingRequestJpaByRequestCode(bookingId);
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
        //todo: find maxQueueOrder
        Integer maxQueueOrder = bookingManageJpaRepository.findMaxQueueOrderBySLotDate(request.getScheduledDate(),request.getScheduledTime());
        booking.setQueueOrder(maxQueueOrder!=null?maxQueueOrder+1:1);
        booking.setCustomerId(customerId);
        booking.setDescription(request.getDescription());
        booking.setIsGuest(request.getIsGuest());
        booking.setStatus(BookingEnum.CONFIRMED);
        booking.setServices(request.getServices());
        booking.setScheduledDate(request.getScheduledDate());
        booking.setScheduledTime(request.getScheduledTime());
        booking.setCreatedAt(request.getCreatedAt());
        List<CatalogItem> catalogItems = request.getServices();
        int estimateTime = catalogItems.stream()
                .filter(item -> item.getServiceService() != null)
                .mapToInt(item -> item.getServiceService().getEstimateTime())
                .sum();
        booking.setEstimateTime(estimateTime);
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

    @Override
    public Boolean reorderQueue(ReorderQueueRequest request) {
        //validate
        List<Integer> orders = request.getOrders().stream()
                .map(QueueOrderItem::getQueueOrder)
                .toList();
        if (orders.size() != orders.stream().distinct().count()) {
            throw new RuntimeException("Thứ tự không được trùng nhau!");
        }

        request.getOrders().forEach(item -> {
            BookingJpa booking = bookingManageJpaRepository.findById(item.getBookingId())
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            booking.setQueueOrder(item.getQueueOrder());
            bookingManageJpaRepository.save(booking);
        });
        return true;
    }

    @Override
    public List<Booking> getBookingBySlot(LocalDate date, LocalTime slot) {
        List<BookingJpa> bookingJpas = bookingManageJpaRepository.findAllBookingBySlotDate(date,slot);
        return bookingJpas.stream().map(bookingManagerMapper::toDomain).toList();
    }

    @Override
    public Booking getBookedById(Integer bookingId) {
        BookingJpa bookingJpa = bookingManageJpaRepository.getBookingJpaByBookingId(bookingId);
        return bookingManagerMapper.toDomain(bookingJpa);
    }

    @Override
    public Booking save(Booking booking) {
        BookingJpa bookingJpa = bookingManageJpaRepository.save(bookingManagerMapper.toBooking(booking));
        return bookingManagerMapper.toDomain(bookingJpa);
    }

}
