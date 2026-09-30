package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface ItemCategoryJpaRepo extends JpaRepository<ItemCategoryJpa, Integer> {

    boolean existsByCategoryCode(String categoryCode);

    ItemCategoryJpa findByCategoryCode(String categoryCode);

    ItemCategoryJpa findFirstByCategoryCode(String categoryCode);

    ItemCategoryJpa findFirstByCategoryNameIgnoreCase(String categoryName);

    List<ItemCategoryJpa> findAllByIsActiveTrueOrderByDisplayOrderAscCategoryNameAsc();

    @Query("SELECT COALESCE(MAX(c.displayOrder), 0) FROM ItemCategoryJpa c")
    int findMaxDisplayOrder();

    interface ItemCategoryProjection {
        Integer getItemCategoryId();
        String getCategoryCode();
        String getCategoryName();
    }

    @Query("""
        select c.itemCategoryId as itemCategoryId, c.categoryCode as categoryCode, c.categoryName as categoryName
            from ItemCategoryJpa c where c.itemCategoryId in :categoryIds
    """)
    List<ItemCategoryProjection> findAllItemCateIdsMap(@Param("categoryIds") Set<Integer> categoryIds);

    default Map<Integer, String> findCateByIds(Set<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return findAllItemCateIdsMap(categoryIds).stream().collect(Collectors.toMap(
                ItemCategoryProjection::getItemCategoryId,
                ItemCategoryProjection::getCategoryCode
        ));
    }

    default Map<Integer, String> findCateNamesByIds(Set<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> result = new java.util.HashMap<>();
        for (ItemCategoryProjection p : findAllItemCateIdsMap(categoryIds)) {
            result.put(p.getItemCategoryId(), p.getCategoryName());
        }
        return result;
    }
}
