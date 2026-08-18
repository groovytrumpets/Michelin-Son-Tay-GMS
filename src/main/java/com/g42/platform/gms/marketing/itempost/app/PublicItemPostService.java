package com.g42.platform.gms.marketing.itempost.app;

import com.g42.platform.gms.marketing.itempost.api.dto.ItemPostDtos;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostTagJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.repository.*;
import com.g42.platform.gms.marketing.itempost.infrastructure.specification.ItemPostSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Đọc dữ liệu bài viết phụ tùng cho phía khách. Chỉ trả bài đã thực sự lên sóng. */
@Service
@RequiredArgsConstructor
public class PublicItemPostService {

    private static final int RELATED_LIMIT = 6;

    private final ItemPostJpaRepo itemPostRepo;
    private final ItemPostCategoryJpaRepo categoryRepo;
    private final ItemPostTagJpaRepo tagRepo;
    private final ItemPostSlugHistoryJpaRepo slugHistoryRepo;
    private final ItemPostMapper mapper;

    @Transactional(readOnly = true)
    public Page<ItemPostDtos.SummaryDto> list(String categorySlug,
                                          String tagSlug,
                                          String keyword,
                                          Boolean featured,
                                          Integer catalogItemId,
                                          Pageable pageable) {
        Specification<ItemPostJpa> spec = ItemPostSpecification.notDeleted()
                .and(ItemPostSpecification.publiclyVisible(LocalDateTime.now()))
                .and(ItemPostSpecification.hasCategorySlug(categorySlug))
                .and(ItemPostSpecification.hasTagSlug(tagSlug))
                .and(ItemPostSpecification.keyword(keyword))
                .and(ItemPostSpecification.isFeatured(featured))
                .and(ItemPostSpecification.hasCatalogItemId(catalogItemId))
                // Thứ tự nằm trong specification, nên pageable phải để không sắp xếp:
                // Sort của pageable được áp sau và sẽ ghi đè thứ tự này.
                .and(ItemPostSpecification.defaultPublicOrder());

        return itemPostRepo.findAll(spec, pageable).map(mapper::toSummary);
    }

    /**
     * Lấy bài theo slug. Nếu slug thuộc lịch sử (biên tập đã đổi đường dẫn) thì
     * vẫn trả bài kèm {@code redirectSlug} để trình duyệt tự chuyển sang link mới.
     */
    @Transactional(readOnly = true)
    public ItemPostDtos.DetailDto getBySlug(String slug) {
        Optional<ItemPostJpa> direct = itemPostRepo.findBySlugAndDeletedAtIsNull(slug);
        if (direct.isPresent()) {
            ItemPostJpa post = direct.get();
            requireVisible(post);
            return mapper.toDetail(post, null);
        }

        ItemPostJpa moved = slugHistoryRepo.findByOldSlug(slug)
                .flatMap(history -> itemPostRepo.findByItemPostIdAndDeletedAtIsNull(history.getItemPostId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));
        requireVisible(moved);
        return mapper.toDetail(moved, moved.getSlug());
    }

    /**
     * Bài viết đã PUBLISHED gắn với một catalog item — dùng để chuyển hướng URL id số cũ
     * ({@code /services/1}) sang URL slug mới ({@code /services/ten-dich-vu}).
     */
    @Transactional(readOnly = true)
    public ItemPostDtos.DetailDto getByCatalogItemId(Integer catalogItemId) {
        ItemPostJpa post = itemPostRepo.findByCatalogItemIdAndDeletedAtIsNull(catalogItemId).stream()
                .filter(ItemPostJpa::isVisibleToPublic)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));
        return mapper.toDetail(post, null);
    }

    /** Thực thể thô — dùng cho trang prerender và đếm view, tránh dựng DTO thừa. */
    @Transactional(readOnly = true)
    public Optional<ItemPostJpa> findVisibleEntity(String slug) {
        Optional<ItemPostJpa> direct = itemPostRepo.findBySlugAndDeletedAtIsNull(slug)
                .filter(ItemPostJpa::isVisibleToPublic);
        if (direct.isPresent()) return direct;

        return slugHistoryRepo.findByOldSlug(slug)
                .flatMap(history -> itemPostRepo.findByItemPostIdAndDeletedAtIsNull(history.getItemPostId()))
                .filter(ItemPostJpa::isVisibleToPublic);
    }

    @Transactional(readOnly = true)
    public List<ItemPostDtos.SummaryDto> related(String slug) {
        ItemPostJpa post = findVisibleEntity(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));

        List<Integer> tagIds = post.getTags().stream().map(ItemPostTagJpa::getTagId).toList();
        Integer categoryId = post.getCategory() == null ? null : post.getCategory().getCategoryId();

        // JPQL không nhận danh sách rỗng trong mệnh đề IN của MySQL, nên đưa vào
        // một giá trị không bao giờ khớp và tắt nhánh tag bằng cờ hasTags.
        List<Integer> safeTagIds = tagIds.isEmpty() ? List.of(-1) : tagIds;

        return itemPostRepo.findRelated(
                        post.getItemPostId(),
                        categoryId,
                        safeTagIds,
                        !tagIds.isEmpty(),
                        LocalDateTime.now(),
                        PageRequest.of(0, RELATED_LIMIT))
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemPostDtos.CategoryDto> activeCategories() {
        return categoryRepo.findByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(category -> mapper.toCategoryDto(category, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemPostDtos.TagDto> popularTags(int limit) {
        return tagRepo.findByOrderByUsageCountDescNameAsc(PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(mapper::toTagDto)
                .toList();
    }

    @Transactional
    public void registerShare(String slug) {
        findVisibleEntity(slug).ifPresent(post -> itemPostRepo.incrementShareCount(post.getItemPostId()));
    }

    private void requireVisible(ItemPostJpa post) {
        if (!post.isVisibleToPublic()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết");
        }
    }
}
