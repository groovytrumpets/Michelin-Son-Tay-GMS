package com.g42.platform.gms.estimation.api.internal;

import com.g42.platform.gms.estimation.domain.entity.Estimate;

import java.util.List;

public interface EstimateInternalApi {
    List<Estimate> findAllByServiceTicketId(List<Integer> ticketIds);

    Estimate findLatestByServiceTicketId(Integer serviceTicketId);

    Estimate findById(Integer estimateId);

    void linkEstimateToServiceTicket(Integer estimateId, Integer serviceTicketId);

    void updateBookingToRemindById(Integer reminderId, Integer bookingId);
}
