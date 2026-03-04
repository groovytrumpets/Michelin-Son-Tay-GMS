package com.g42.platform.gms.booking_management.api.dto.timeslot;

import com.g42.platform.gms.booking_management.api.dto.requesting.BookingRequestRes;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotBookedListResponse {
    private LocalDate date;
    private List<SlotBookedResponse> bookingRequestResList;
}
