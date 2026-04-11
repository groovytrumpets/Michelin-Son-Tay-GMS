package com.g42.platform.gms.service_ticket_management.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TicketPaidEvent extends ApplicationEvent {

    private final Integer serviceTicketId;
    private final Integer staffId;

    public TicketPaidEvent(Object source, Integer serviceTicketId, Integer staffId) {
        super(source);
        this.serviceTicketId = serviceTicketId;
        this.staffId = staffId;
    }
}
