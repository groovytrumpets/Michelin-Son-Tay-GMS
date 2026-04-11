package com.g42.platform.gms.warehouse.app.event;

import com.g42.platform.gms.service_ticket_management.domain.event.EstimateApprovedEvent;
import com.g42.platform.gms.service_ticket_management.domain.event.TicketCancelledEvent;
import com.g42.platform.gms.service_ticket_management.domain.event.TicketPaidEvent;
import com.g42.platform.gms.warehouse.app.service.allocation.StockAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseAllocationListener {

    private final StockAllocationService stockAllocationService;

    /** Khi estimate APPROVED → reserve hàng */
    @EventListener
    @Async
    public void onEstimateApproved(EstimateApprovedEvent event) {
        try {
            var shortages = stockAllocationService.reserve(event.getEstimateId(), event.getStaffId());
            if (!shortages.isEmpty()) {
                log.warn("Stock shortages detected for estimateId={}: {}", event.getEstimateId(), shortages);
            }
        } catch (Exception e) {
            log.error("Failed to reserve stock for estimateId={}", event.getEstimateId(), e);
        }
    }

    /** Khi ticket PAID → commit hàng */
    @EventListener
    @Async
    public void onTicketPaid(TicketPaidEvent event) {
        try {
            stockAllocationService.commit(event.getServiceTicketId(), event.getStaffId());
        } catch (Exception e) {
            log.error("Failed to commit stock for ticketId={}", event.getServiceTicketId(), e);
        }
    }

    /** Khi ticket CANCELLED → release hàng */
    @EventListener
    @Async
    public void onTicketCancelled(TicketCancelledEvent event) {
        try {
            stockAllocationService.release(event.getServiceTicketId(), event.getStaffId());
        } catch (Exception e) {
            log.error("Failed to release stock for ticketId={}", event.getServiceTicketId(), e);
        }
    }
}
