package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WorkCategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface WorkCategoryJpaEntityRepo extends JpaRepository<WorkCategoryJpaEntity,Integer> {
    boolean existsByCategoryCode(String categoryCode);

    WorkCategoryJpaEntity findByCategoryCode(String categoryCode);

    WorkCategoryJpaEntity findByCategoryCodeContainingIgnoreCase(String categoryCode);
    @Query("SELECT COALESCE(MAX(wc.displayOrder), 0) FROM WorkCategoryJpa wc")
    int findMaxDisplayOrder();


    interface ItemCategoryProjection{
        Integer getWorkCategoryId();
        String getCategoryCode();
    }
    @Query("""
    select c.workCategoryId as workCategoryId, c.categoryCode as categoryCode 
        from WorkCategoryJpaEntity c where c.workCategoryId in :categoryIds
    """)
    List<ItemCategoryProjection> findAllItemCateIdsMap(@Param("categoryIds")Set<Integer> categoryIds);


    default Map<Integer, String> findCateByIds(Set<java.lang.Integer> categoryIds){
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
    List<WorkCategoryJpaEntityRepo.ItemCategoryProjection> projection = findAllItemCateIdsMap(categoryIds);
        return projection.stream().collect(Collectors.toMap(
                WorkCategoryJpaEntityRepo.ItemCategoryProjection::getWorkCategoryId,
                WorkCategoryJpaEntityRepo.ItemCategoryProjection::getCategoryCode
        ));

    };


}
