package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.infrastructure.entity.ServiceReminderJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.EstimateJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
import com.g42.platform.gms.estimation.infrastructure.repository.ServiceRemindJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstimateInternalApiImpl implements EstimateInternalApi {
    @Autowired
    private EstimateRepositoryJpa estimateRepositoryJpa;
    @Autowired
    private EstimateJpaMapper estimateJpaMapper;
    @Autowired
    private ServiceRemindJpaRepo serviceRemindJpaRepo;

    @Override
    public List<Estimate> findAllByServiceTicketId(List<Integer> ticketIds) {
        return estimateRepositoryJpa.findByServiceTicketIdsAndVersionTop(ticketIds).stream().map(estimateJpaMapper::toDomain).toList();
    }

    @Override
    public Estimate findLatestByServiceTicketId(Integer serviceTicketId) {
        if (serviceTicketId == null) {
            return null;
        }
        return estimateRepositoryJpa.findTopByServiceTicketIdOrderByVersionDesc(serviceTicketId)
                .map(estimateJpaMapper::toDomain)
                .orElse(null);
    }

    @Override
    public void updateBookingToRemindById(Integer reminderId, Integer bookingId) {
        ServiceReminderJpa sr = serviceRemindJpaRepo.findById(reminderId).orElse(null);
        if (sr==null||bookingId==null) {
            System.err.println("INVALID REMINDER/BOOKING ID");
            return;
        }
        sr.setStatus("BOOKED");
        sr.setBookingId(bookingId);
        serviceRemindJpaRepo.save(sr);
    }
}
