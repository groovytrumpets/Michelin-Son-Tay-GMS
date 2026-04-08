package com.g42.platform.gms.service_ticket_management.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EstimateApprovedEvent extends ApplicationEvent {

    private final Integer estimateId;
    private final Integer serviceTicketId;
    private final Integer staffId;

    public EstimateApprovedEvent(Object source, Integer estimateId, Integer serviceTicketId, Integer staffId) {
        super(source);
        this.estimateId = estimateId;
        this.serviceTicketId = serviceTicketId;
        this.staffId = staffId;
    }
}
