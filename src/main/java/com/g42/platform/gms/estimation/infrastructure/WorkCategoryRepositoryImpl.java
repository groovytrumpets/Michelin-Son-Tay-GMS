package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.domain.repository.WorkCategoryRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.WorkCategoryJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.WorkCategoryRepositoryJpa;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class WorkCategoryRepositoryImpl implements WorkCategoryRepository {
    private final WorkCategoryRepositoryJpa workCategoryRepositoryJpa;
    private final WorkCategoryJpaMapper workCategoryJpaMapper;

    @Override
    public List<WorkCategory> findAllById(Iterable<Integer> workCategoryId) {
        List<WorkCategoryJpa> workCategoryJpas = workCategoryRepositoryJpa.findAllById(workCategoryId);
        return workCategoryJpas.stream().map(workCategoryJpaMapper::toDomain).toList();
    }

    @Override
    public WorkCategory save(WorkCategory newCategory) {
        WorkCategoryJpa workCategoryJpa = workCategoryJpaMapper.toJpa(newCategory);
        workCategoryRepositoryJpa.save(workCategoryJpa);

        return workCategoryRepositoryJpa.findById(workCategoryJpa.getId()).map(workCategoryJpaMapper::toDomain).orElse(null);
    }

    @Override
    public int findMaxDisplayOrder() {
        return workCategoryRepositoryJpa.findMaxDisplayOrder();
    }

    @Override
    public WorkCategory findById(Integer categoryId) {
        WorkCategoryJpa workCategoryJpa = workCategoryRepositoryJpa.findByIdWork(categoryId);
        return workCategoryJpaMapper.toDomain(workCategoryJpa);
    }

    /**
     * Trả về mọi hạng mục đang hoạt động, không chỉ hạng mục mặc định.
     * Bảng báo giá dùng danh sách này để tra ngược hạng mục của sản phẩm, nên
     * nếu lọc theo is_default thì hạng mục do người dùng tạo sẽ không tra được
     * và dòng báo giá bị báo thiếu hạng mục.
     */
    @Override
    public List<WorkCategory> findAll() {
        List<WorkCategoryJpa> workCategoryJpas =
                workCategoryRepositoryJpa.findAllByIsActiveTrueOrderByDisplayOrderAscCategoryNameAsc();
        return workCategoryJpas.stream().map(workCategoryJpaMapper::toDomain).toList();
    }

    @Override
    public WorkCategory findByCategoryName(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        return workCategoryRepositoryJpa.findByCategoryNameIgnoreCase(categoryName).stream()
                .findFirst()
                .map(workCategoryJpaMapper::toDomain)
                .orElse(null);
    }

    @Override
    public List<WorkCategory> findAllIncludingInactive() {
        return workCategoryRepositoryJpa.findAll().stream().map(workCategoryJpaMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Integer categoryId) {
        workCategoryRepositoryJpa.deleteById(categoryId);
    }
}

