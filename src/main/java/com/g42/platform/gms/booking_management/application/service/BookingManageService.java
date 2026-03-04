package com.g42.platform.gms.booking_management.application.service;



import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.api.dto.TimeSlotResponse;
import com.g42.platform.gms.booking.customer.domain.entity.SlotReservation;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedDetailResponse;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.api.dto.requesting.*;
import com.g42.platform.gms.booking_management.api.dto.timeslot.SlotBookedResponse;
import com.g42.platform.gms.booking_management.api.mapper.BookingMDetailDtoMapper;
import com.g42.platform.gms.booking_management.api.mapper.BookingMRequestDtoMapper;
import com.g42.platform.gms.booking_management.api.mapper.BookingManageDtoMapper;
import com.g42.platform.gms.booking_management.application.command.CreateCustomerCommand;
import com.g42.platform.gms.booking_management.application.port.CustomerGateway;
import com.g42.platform.gms.booking_management.domain.entity.*;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.domain.exception.BookingStaffErrorCode;
import com.g42.platform.gms.booking_management.domain.exception.BookingStaffException;
import com.g42.platform.gms.booking_management.domain.repository.BookingManageRepository;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.mapper.TimeSlotMMapper;
import com.g42.platform.gms.marketing.service_catalog.domain.exception.ServiceException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class BookingManageService {
    private static final int BASE_SLOT_MINUTES = 30;
    private final BookingManageRepository bookingRepository;
    private final BookingManageDtoMapper bookingManageDtoMapper;
    private final BookingMDetailDtoMapper  bookingMDetailDtoMapper;
    private final BookingMRequestDtoMapper  bookingMRequestDtoMapper;

    public Page<BookedRespond> getListBooked(int page, int size, LocalDate date, Boolean isGuest, BookingEnum status, String search) {
        Page<BookedRespond> bookingPage = bookingRepository.getBookedList(page,size,date,isGuest,status,search);
//        return  bookingRepository.getBookedList().stream().map(bookingManageDtoMapper::toBookedRespond).toList();
        return bookingPage;
    }

    public BookedDetailResponse getBookedDetailById(String bookingId) {
        return bookingManageDtoMapper.toBookedDetailResponse(bookingRepository.getBookedDetailById(bookingId));
        //todo: null handle exception
    }

    public Page<BookingRequestRes> getListBookingRequest(int page, int size, LocalDate date, Boolean isGuest, BookingRequestStatus status, String search) {
        Page<BookingRequest> bookingList = bookingRepository.getBookingRequestList(page,size,date,isGuest,status,search);
        //return bookingMRequestDtoMapper.toBookingRequestResPage(bookingList);
        return bookingList.map(bookingMRequestDtoMapper::toBookingRequestRes);
    }

    public BookingRequestDetailRes getBookingRequestById(String bookingCode) {
        BookingRequest bookingRequest = bookingRepository.getBookingRequestById(bookingCode);
        return bookingMRequestDtoMapper.toBookingRequestDetailRes(bookingRequest);
    }
    private final CustomerGateway customerGateway;
    @Transactional
    public Boolean confirmBookingRequest(String requestId) {
        BookingRequest request = bookingRepository.getBookingRequestById(requestId);
        if (!request.isPending()){
            //todo: wrong status handle
        }
        //todo: check slot capacity
        System.out.println("request slot: " + request.getScheduledTime());
            //todo: search for reservedCount
        int reservedCount = bookingRepository.countReserverdBasedOnTime(request.getScheduledTime());
        System.out.println("reserved count: " + reservedCount);
            //todo: compare reservedCount to available slot
        TimeSlot timeSlot = bookingRepository.getTimeSlotByTime(request.getScheduledTime());
        System.out.println("time slot: " + timeSlot);
        //todo: check acc available else create customer acc
        int customerId = customerGateway.getOrCreateCustomer(
                new CreateCustomerCommand(request.getFullName(),request.getPhone(),request.getCreatedAt())
        );
        System.out.println("customer id: " + customerId);
        //todo: check if create account success
        //todo: create booking
        BookingJpa booking = bookingRepository.createBookingByRequest(request, customerId);
        System.out.println("booking: " + booking.getBookingId());
        //todo: create Reservation
        BookingSlotReservation bookingSlotReservation = bookingRepository.createBookingSlotReservation(request, booking);
        System.out.println("booking slot reservation: " + bookingSlotReservation);
        //todo: update booking request status
        boolean confirmed = request.confirm();
        bookingRepository.setConfirmStatus(request);
return confirmed;
    }
    private final TimeSlotMMapper  timeSlotMMapper;
    public List<TimeSlot> getListTimeSlotByBookingId(Integer bookingId) {
//        List<TimeSlotJpa> list = bookingRepository.getListOfTimeSlotByBookingId(bookingId);
//        return timeSlotMMapper.toDomainTimeSlotList(list);
        return null;
    }
    @Transactional(noRollbackFor = BookingStaffException.class)
    public ActionBookingRespond cancelBookingRequest(String requestId, ActionBookingRequest actionBookingRequest) {
        BookingRequest request = bookingRepository.getBookingRequestById(requestId);
        if (request==null){
            throw new BookingStaffException("LOI", BookingStaffErrorCode.INVALID_ID);
        }
        request.cancel(actionBookingRequest.getReason(), actionBookingRequest.getNote());
        bookingRepository.setRequestBooking(request);
        return new ActionBookingRespond("SUCCESS","SUCCESS");
    }
    @Transactional(noRollbackFor = BookingStaffException.class)
    public ActionBookingRespond spamNotedBookingRequest(String requestId, ActionBookingRequest actionBookingRequest) {
        BookingRequest request = bookingRepository.getBookingRequestById(requestId);
        if (request==null){
            throw new BookingStaffException("Không tìm thấy booking", BookingStaffErrorCode.INVALID_ID);
        }
        request.spam(actionBookingRequest.getReason(), actionBookingRequest.getNote());
        bookingRepository.setRequestBooking(request);
        return new ActionBookingRespond("SUCCESS","SUCCESS");
    }

    public ActionBookingRespond contactedBookingRequest(String requestId, ActionBookingRequest actionBookingRequest) {
        BookingRequest request = bookingRepository.getBookingRequestById(requestId);
        if (request==null){
            throw new BookingStaffException("Không tìm thấy booking", BookingStaffErrorCode.INVALID_ID);
        }
        request.contacted(actionBookingRequest.getReason(), actionBookingRequest.getNote());
        bookingRepository.setRequestBooking(request);
        return new ActionBookingRespond("SUCCESS","SUCCESS");
    }

    public Boolean updateBookingRequest(String requestId, BookingRequestUpdateReq actionBookingRequest) {
        BookingRequest request = bookingRepository.getBookingRequestById(requestId);
        if (request==null){
            throw new BookingStaffException("Không tìm thấy booking", BookingStaffErrorCode.INVALID_ID);
        }
        if (request.cantCancel()){
            throw new BookingStaffException("Booking này đã xử lý rồi!", BookingStaffErrorCode.BOOKING_CANT_EDIT);
        }
        request.setScheduledDate(actionBookingRequest.getScheduledDate());
        request.setScheduledTime(actionBookingRequest.getScheduledTime());
        request.setDescription(actionBookingRequest.getDescription());
        request.setServiceCategory(actionBookingRequest.getServiceCategory());
        request.setIsGuest(actionBookingRequest.getIsGuest());
        List<CatalogItem> catalogItems = new ArrayList<>();
        catalogItems = bookingRepository.getListOfCatalogById(actionBookingRequest.getServices());
        request.setServices(catalogItems);


        bookingRepository.setRequestBooking(request);
        return true;
    }

    public List<SlotBookedResponse> getAvailableSlotsForBooking(LocalDate date, int durationMinutes) {
        List<SlotBookedResponse> slotBookedRes = new ArrayList<>();
        List<TimeSlot> allSlot = bookingRepository.getAllSlotTime();
        for (TimeSlot slotConfig : allSlot) {
            LocalTime slotTime = slotConfig.getStartTime();
            LocalDateTime slotDateTime = LocalDateTime.of(date, slotTime);


                int remainingCapacity = slotConfig.getCapacity();

                SlotBookedResponse dto = new SlotBookedResponse();
                dto.setSlotId(slotConfig.getSlotId());
                dto.setStartTime(slotTime);
                dto.setPeriod(slotConfig.getPeriod());
                dto.setCapacity(slotConfig.getCapacity());
                dto.setIsActive(slotConfig.getIsActive());
                dto.setRemainingCapacity(remainingCapacity);
                dto.setIsAvailable(true);
                dto.setStatus("Còn trống");

                slotBookedRes.add(dto);
            }
        return  slotBookedRes;
        }
    }


