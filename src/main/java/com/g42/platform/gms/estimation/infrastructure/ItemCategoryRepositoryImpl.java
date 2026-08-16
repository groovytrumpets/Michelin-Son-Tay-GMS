package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.ItemCategory;
import com.g42.platform.gms.estimation.domain.repository.ItemCategoryRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.ItemCategoryJpaEntity;
import com.g42.platform.gms.estimation.infrastructure.mapper.ItemCategoryJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.ItemCategoryRepositoryJpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class ItemCategoryRepositoryImpl implements ItemCategoryRepository {

    private final ItemCategoryRepositoryJpa itemCategoryRepositoryJpa;
    private final ItemCategoryJpaMapper itemCategoryJpaMapper;

    @Override
    public List<ItemCategory> findAllById(Iterable<Integer> itemCategoryIds) {
        return itemCategoryRepositoryJpa.findAllById(itemCategoryIds).stream()
                .map(itemCategoryJpaMapper::toDomain)
                .toList();
    }

    @Override
    public ItemCategory findById(Integer categoryId) {
        if (categoryId == null) return null;
        ItemCategoryJpaEntity entity = itemCategoryRepositoryJpa.findById(categoryId).orElse(null);
        return entity == null ? null : itemCategoryJpaMapper.toDomain(entity);
    }

    /**
     * Trả mọi danh mục đang hoạt động. Bảng báo giá dùng danh sách này để tra ngược
     * danh mục của sản phẩm, nên không được lọc hẹp hơn — lọc thiếu là dòng báo giá
     * mất tên nhóm.
     */
    @Override
    public List<ItemCategory> findAll() {
        return itemCategoryRepositoryJpa.findAllByIsActiveTrueOrderByDisplayOrderAscCategoryNameAsc().stream()
                .map(itemCategoryJpaMapper::toDomain)
                .toList();
    }

    @Override
    public ItemCategory findByCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        return itemCategoryRepositoryJpa.findByCategoryNameIgnoreCase(categoryName).stream()
                .findFirst()
                .map(itemCategoryJpaMapper::toDomain)
                .orElse(null);
    }
}
