package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;

import java.util.List;
import java.util.Map;

/**
 * Domain repo cho catalog item dạng PART — tách biệt với CatalogItemRepo cũ (dùng domain entity).
 * Service warehouse dùng interface này thay vì inject CatalogItemJpaRepo trực tiếp.
 */
public interface PartCatalogRepo {

    List<CatalogItem> findAllParts();

    List<CatalogItem> searchParts(String keyword);

    List<CatalogItem> findAllPartsByIds(List<Integer> ids);

    boolean existsBySku(String sku);

    CatalogItem save(CatalogItem item);

    /** Lấy tên item theo danh sách id — dùng cho enrichment response */
    Map<Integer, String> findNamesByIds(List<Integer> ids);
}
