package com.g42.platform.gms.estimation.api.internal;

import com.g42.platform.gms.estimation.domain.entity.Estimate;

import java.util.List;

public interface EstimateInternalApi {
    List<Estimate> findAllByServiceTicketId(List<Integer> ticketIds);

    void updateBookingToRemindById(Integer reminderId, Integer bookingId);
}
