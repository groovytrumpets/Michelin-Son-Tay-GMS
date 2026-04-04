package com.g42.platform.gms.service_ticket_management.api.internal;

import org.springframework.stereotype.Repository;

@Repository
public interface ServiceTicketInternalApi {
    Integer getServiceIdByCode(String trackingId);
}
