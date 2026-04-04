package com.g42.platform.gms.service_ticket_management.infrastructure;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.service_ticket_management.api.internal.ServiceTicketInternalApi;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceTicketInternalApiImpl implements ServiceTicketInternalApi {
    @Autowired
    private ServiceTicketRepository serviceTicketRepository;

    @Override
    public Integer getServiceIdByCode(String trackingId) {
        try {
        ServiceJpaEntity serviceJpaEntity = serviceTicketRepository.findServiceTicketJpasByTicketCode(trackingId);
        if (serviceJpaEntity == null) {
            return -1;
        }
        return Integer.parseInt(serviceJpaEntity.getServiceId().toString());

        }catch (Exception e) {
            System.err.println("Exception in ServiceTicketInternalApiImpl.getServiceIdByCode: "+e.getMessage());
            return -1;
        }
    }
}
