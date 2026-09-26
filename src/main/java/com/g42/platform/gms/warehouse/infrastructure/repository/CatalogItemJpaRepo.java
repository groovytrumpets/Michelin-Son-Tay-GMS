package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CatalogItemJpaRepo extends JpaRepository<CatalogItemJpa,Integer>, JpaSpecificationExecutor<CatalogItemJpa> {
    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    /*
     * SKU/slug chỉ tính là "đã có người dùng" khi thuộc mục đang hoạt động — mục đã xoá
     * (is_active = 0) nhường lại cho mục mới. is_active NULL là dữ liệu cũ, coi như đang hoạt động.
     * excludeId: bỏ qua chính mục đang sửa/kích hoạt lại (truyền null khi tạo mới).
     */
    @Query("select case when count(c) > 0 then true else false end from WarehouseCatalogItem c where c.sku = :sku"
            + " and (c.isActive is null or c.isActive = true)"
            + " and (:excludeId is null or c.itemId <> :excludeId)")
    boolean existsActiveBySku(@Param("sku") String sku, @Param("excludeId") Integer excludeId);

    @Query("select case when count(c) > 0 then true else false end from WarehouseCatalogItem c where c.slug = :slug"
            + " and (c.isActive is null or c.isActive = true)"
            + " and (:excludeId is null or c.itemId <> :excludeId)")
    boolean existsActiveBySlug(@Param("slug") String slug, @Param("excludeId") Integer excludeId);

    /**
     * Gỡ slug khỏi mục đã xoá để mục mới lấy lại được — cột slug có unique index nên
     * phải chạy UPDATE này (ngay lập tức, không chờ flush) trước khi ghi mục mới.
     */
    @Modifying(flushAutomatically = true)
    @Query("update WarehouseCatalogItem c set c.slug = null where c.slug = :slug and c.isActive = false")
    int releaseSlugFromInactive(@Param("slug") String slug);

    java.util.Optional<CatalogItemJpa> findBySlug(String slug);

    List<CatalogItemJpa> findByItemType(CatalogItemType itemType);

    List<CatalogItemJpa> findByItemTypeAndItemIdIn(CatalogItemType itemType, List<Integer> ids);

    boolean existsByBrandId(Integer brandId);

    boolean existsByProductLineId(Integer productLineId);

    boolean existsByItemCategoryId(Integer itemCategoryId);

    boolean existsByUnit(String unit);

    List<CatalogItemJpa> findByServiceId(Long serviceId);
}
