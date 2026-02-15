package com.g42.platform.gms.booking_management.application.service;



import com.g42.platform.gms.booking_management.api.dto.BookedDetailResponse;
import com.g42.platform.gms.booking_management.api.dto.BookedRespond;
import com.g42.platform.gms.booking_management.api.mapper.BookingMDetailDtoMapper;
import com.g42.platform.gms.booking_management.api.mapper.BookingManageDtoMapper;
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

    public List<BookedRespond> getListBooked() {

        return  bookingRepository.getBookedList().stream().map(bookingManageDtoMapper::toBookedRespond).toList();
    }

    public BookedDetailResponse getBookedDetailById(Integer bookingId) {
        return bookingManageDtoMapper.toBookedDetailResponse(bookingRepository.getBookedDetailById(bookingId));
        //todo: null handle exception
    }
}
