package com.g42.platform.gms.booking_management.application.service;



import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedDetailResponse;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.api.dto.requesting.ActionBookingRespond;
import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestDetailRes;
import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestRes;
import com.g42.platform.gms.booking_management.api.dto.requesting.ActionBookingRequest;
import com.g42.platform.gms.booking_management.api.mapper.BookingMDetailDtoMapper;
import com.g42.platform.gms.booking_management.api.mapper.BookingMRequestDtoMapper;
import com.g42.platform.gms.booking_management.api.mapper.BookingManageDtoMapper;
import com.g42.platform.gms.booking_management.application.port.CustomerGateway;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequest;
import com.g42.platform.gms.booking_management.domain.entity.BookingSlotReservation;
import com.g42.platform.gms.booking_management.domain.entity.TimeSlot;
import com.g42.platform.gms.booking_management.domain.exception.BookingStaffErrorCode;
import com.g42.platform.gms.booking_management.domain.exception.BookingStaffException;
import com.g42.platform.gms.booking_management.domain.repository.BookingManageRepository;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.mapper.TimeSlotMMapper;
import com.g42.platform.gms.marketing.service_catalog.domain.exception.ServiceException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
public class BookingManageService {
    private final BookingManageRepository bookingRepository;
    private final BookingManageDtoMapper bookingManageDtoMapper;
    private final BookingMDetailDtoMapper  bookingMDetailDtoMapper;
    private final BookingMRequestDtoMapper  bookingMRequestDtoMapper;

    public List<BookedRespond> getListBooked() {

        return  bookingRepository.getBookedList().stream().map(bookingManageDtoMapper::toBookedRespond).toList();
    }

    public BookedDetailResponse getBookedDetailById(Integer bookingId) {
        return bookingManageDtoMapper.toBookedDetailResponse(bookingRepository.getBookedDetailById(bookingId));
        //todo: null handle exception
    }

    public List<BookingRequestRes> getListBookingRequest() {
        List<BookingRequest> bookingList = bookingRepository.getBookingRequestList();
        return bookingMRequestDtoMapper.toBookingRequestRes(bookingList);
    }

    public BookingRequestDetailRes getBookingRequestById(Integer bookingId) {
        BookingRequest bookingRequest = bookingRepository.getBookingRequestById(bookingId);
        return bookingMRequestDtoMapper.toBookingRequestDetailRes(bookingRequest);
    }
    private final CustomerGateway customerGateway;
    @Transactional
    public Boolean confirmBookingRequest(Integer requestId) {
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
//        int customerId = customerGateway.getOrCreateCustomer(
//                new CreateCustomerCommand(request.getFullName(),request.getPhone(),request.getCreatedAt())
//        );
//        System.out.println("customer id: " + customerId);
        //todo: check if create account success
        //todo: create booking
        BookingJpa booking = bookingRepository.createBookingByRequest(request);
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
    public ActionBookingRespond cancelBookingRequest(Integer requestId, ActionBookingRequest actionBookingRequest) {
        BookingRequest request = bookingRepository.getBookingRequestById(requestId);
        if (request==null){
            throw new BookingStaffException("LOI", BookingStaffErrorCode.INVALID_ID);
        }
        request.cancel(actionBookingRequest.getReason(), actionBookingRequest.getNote());
        bookingRepository.setRequestBooking(request);
        return new ActionBookingRespond("SUCCESS","SUCCESS");
    }
    @Transactional(noRollbackFor = BookingStaffException.class)
    public ActionBookingRespond spamNotedBookingRequest(Integer requestId, ActionBookingRequest actionBookingRequest) {
        BookingRequest request = bookingRepository.getBookingRequestById(requestId);
        if (request==null){
            throw new BookingStaffException("Không tìm thấy booking", BookingStaffErrorCode.INVALID_ID);
        }
        request.spam(actionBookingRequest.getReason(), actionBookingRequest.getNote());
        bookingRepository.setRequestBooking(request);
        return new ActionBookingRespond("SUCCESS","SUCCESS");
    }

    public ActionBookingRespond contactedBookingRequest(Integer requestId, ActionBookingRequest actionBookingRequest) {
        BookingRequest request = bookingRepository.getBookingRequestById(requestId);
        if (request==null){
            throw new BookingStaffException("Không tìm thấy booking", BookingStaffErrorCode.INVALID_ID);
        }
        request.contacted(actionBookingRequest.getReason(), actionBookingRequest.getNote());
        bookingRepository.setRequestBooking(request);
        return new ActionBookingRespond("SUCCESS","SUCCESS");
    }
}
