package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface ItemCategoryJpaRepo extends JpaRepository<ItemCategoryJpa,Integer> {
    boolean existsByCategoryCode(String categoryCode);

    ItemCategoryJpa findByCategoryCode(String categoryCode);

    ItemCategoryJpa findByCategoryCodeContainingIgnoreCase(String categoryCode);


    interface ItemCategoryProjection{
        Integer getItemCategoryId();
        String getCategoryCode();
    }
    @Query("""
    select c.itemCategoryId as itemCategoryId, c.categoryCode as categoryCode 
        from ItemCategoryJpa c where c.itemCategoryId in :categoryIds
    """)
    List<ItemCategoryProjection> findAllItemCateIdsMap(@Param("categoryIds")Set<Integer> categoryIds);


    default Map<Integer, String> findCateByIds(Set<java.lang.Integer> categoryIds){
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
    List<ItemCategoryJpaRepo.ItemCategoryProjection> projection = findAllItemCateIdsMap(categoryIds);
        return projection.stream().collect(Collectors.toMap(
                ItemCategoryJpaRepo.ItemCategoryProjection::getItemCategoryId,
                ItemCategoryJpaRepo.ItemCategoryProjection::getCategoryCode
        ));

    };


}
