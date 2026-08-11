package com.g42.platform.gms.marketing.news.app;

import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostCategoryJpa;
import com.g42.platform.gms.marketing.news.infrastructure.repository.PostCategoryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** Quản lý danh mục tin tức. */
@Service
@RequiredArgsConstructor
public class PostCategoryService {

    private final PostCategoryJpaRepo categoryRepo;
    private final PostMapper mapper;

    @Transactional(readOnly = true)
    public List<PostDtos.CategoryDto> listAll() {
        return categoryRepo.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(category -> mapper.toCategoryDto(category, null))
                .toList();
    }

    @Transactional
    public PostDtos.CategoryDto create(PostDtos.CategorySaveRequest request) {
        PostCategoryJpa category = new PostCategoryJpa();
        category.setCreatedAt(LocalDateTime.now());
        category.setSlug(SlugGenerator.toUniqueSlug(
                request.slug(), request.name(), categoryRepo::existsBySlug));
        apply(category, request);
        return mapper.toCategoryDto(categoryRepo.save(category), null);
    }

    @Transactional
    public PostDtos.CategoryDto update(Integer categoryId, PostDtos.CategorySaveRequest request) {
        PostCategoryJpa category = categoryRepo.findById(categoryId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"));

        String desired = SlugGenerator.toSlug(
                request.slug() == null || request.slug().isBlank() ? request.name() : request.slug());
        if (!desired.isEmpty() && !desired.equals(category.getSlug())) {
            category.setSlug(SlugGenerator.toUniqueSlug(desired, request.name(), categoryRepo::existsBySlug));
        }

        apply(category, request);
        return mapper.toCategoryDto(categoryRepo.save(category), null);
    }

    /**
     * Xoá danh mục. Khoá ngoại đặt ON DELETE SET NULL nên bài thuộc danh mục này
     * không mất, chỉ trở thành chưa phân loại.
     */
    @Transactional
    public void delete(Integer categoryId) {
        if (!categoryRepo.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục");
        }
        categoryRepo.deleteById(categoryId);
    }

    private void apply(PostCategoryJpa category, PostDtos.CategorySaveRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        category.setIsActive(request.isActive() == null || request.isActive());
        category.setSeoTitle(request.seoTitle());
        category.setSeoDescription(request.seoDescription());
        category.setUpdatedAt(LocalDateTime.now());
    }
}
