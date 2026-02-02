package com.g42.platform.gms.catalog.repository;

import com.g42.platform.gms.catalog.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Integer> {
    // Tìm các dịch vụ đang hoạt động để hiển thị lên App
    List<CatalogItem> findByIsActiveTrue();
}