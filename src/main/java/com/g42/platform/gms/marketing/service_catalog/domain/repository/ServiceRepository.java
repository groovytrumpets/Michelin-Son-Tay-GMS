package com.g42.platform.gms.marketing.service_catalog.domain.repository;

import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;

import java.util.List;

public interface ServiceRepository {
    List<Service> findAllActive();
}
