package com.g42.platform.gms.marketing.service_catalog.domain.repository;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface ServiceRepository {
    List<Service> findAllActive();

    Service findServiceDetailById(Long serviceId);

    Long[] getCatalogIdByServiceId(Long[] serviceId);

    Service save(Service service);

    Page<Service> getListOfProductsByCatalogItem(int page, int size, CatalogItemType itemType, String search, String sortBy, BigDecimal maxPrice, BigDecimal minPrice, Integer categoryCode, Integer brandId, Integer productLineId);
}
