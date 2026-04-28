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
        // TEMP DISABLED: Reserve ownership is handled by estimation flow.
        // Avoid duplicate reserved_quantity updates from warehouse listener.
        // Old logic (keep for future re-enable):
        // try {
        //     stockAllocationService.reserve(event.getEstimateId(), event.getStaffId());
        // } catch (Exception e) {
        //     log.error("Failed to reserve stock for estimateId={}", event.getEstimateId(), e);
        // }
        log.info("Skip warehouse auto-reserve for estimateId={} because estimate is reserve owner", event.getEstimateId());
    }

    /**
     * Khi ticket PAID: luồng mới không tự động commit/xuat kho.
     * Advisor phải chủ động gọi API request-issue, kho xác nhận riêng.
     */
    @EventListener
    @Async
    public void onTicketPaid(TicketPaidEvent event) {
        log.info("Ticket {} paid - skip auto warehouse commit in manual request-issue flow",
                event.getServiceTicketId());
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
