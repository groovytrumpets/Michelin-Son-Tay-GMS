package com.g42.platform.gms.booking_management.application.service;



import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedDetailResponse;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestDetailRes;
import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestRes;
import com.g42.platform.gms.booking_management.api.mapper.BookingMDetailDtoMapper;
import com.g42.platform.gms.booking_management.api.mapper.BookingMRequestDtoMapper;
import com.g42.platform.gms.booking_management.api.mapper.BookingManageDtoMapper;
import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequest;
import com.g42.platform.gms.booking_management.domain.repository.BookingManageRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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
}
