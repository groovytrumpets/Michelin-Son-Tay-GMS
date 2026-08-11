package com.g42.platform.gms.marketing.news.app;

import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostJpa;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostTagJpa;
import com.g42.platform.gms.marketing.news.infrastructure.repository.*;
import com.g42.platform.gms.marketing.news.infrastructure.specification.PostSpecification;
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

/** Đọc dữ liệu tin tức cho phía khách. Chỉ trả bài đã thực sự lên sóng. */
@Service
@RequiredArgsConstructor
public class PublicPostService {

    private static final int RELATED_LIMIT = 6;

    private final PostJpaRepo postRepo;
    private final PostCategoryJpaRepo categoryRepo;
    private final PostTagJpaRepo tagRepo;
    private final PostSlugHistoryJpaRepo slugHistoryRepo;
    private final PostMapper mapper;

    @Transactional(readOnly = true)
    public Page<PostDtos.SummaryDto> list(String categorySlug,
                                          String tagSlug,
                                          String keyword,
                                          Boolean featured,
                                          Pageable pageable) {
        Specification<PostJpa> spec = PostSpecification.notDeleted()
                .and(PostSpecification.publiclyVisible(LocalDateTime.now()))
                .and(PostSpecification.hasCategorySlug(categorySlug))
                .and(PostSpecification.hasTagSlug(tagSlug))
                .and(PostSpecification.keyword(keyword))
                .and(PostSpecification.isFeatured(featured))
                // Thứ tự nằm trong specification, nên pageable phải để không sắp xếp:
                // Sort của pageable được áp sau và sẽ ghi đè thứ tự này.
                .and(PostSpecification.defaultPublicOrder());

        return postRepo.findAll(spec, pageable).map(mapper::toSummary);
    }

    /**
     * Lấy bài theo slug. Nếu slug thuộc lịch sử (biên tập đã đổi đường dẫn) thì
     * vẫn trả bài kèm {@code redirectSlug} để trình duyệt tự chuyển sang link mới.
     */
    @Transactional(readOnly = true)
    public PostDtos.DetailDto getBySlug(String slug) {
        Optional<PostJpa> direct = postRepo.findBySlugAndDeletedAtIsNull(slug);
        if (direct.isPresent()) {
            PostJpa post = direct.get();
            requireVisible(post);
            return mapper.toDetail(post, null);
        }

        PostJpa moved = slugHistoryRepo.findByOldSlug(slug)
                .flatMap(history -> postRepo.findByPostIdAndDeletedAtIsNull(history.getPostId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));
        requireVisible(moved);
        return mapper.toDetail(moved, moved.getSlug());
    }

    /** Thực thể thô — dùng cho trang prerender và đếm view, tránh dựng DTO thừa. */
    @Transactional(readOnly = true)
    public Optional<PostJpa> findVisibleEntity(String slug) {
        Optional<PostJpa> direct = postRepo.findBySlugAndDeletedAtIsNull(slug)
                .filter(PostJpa::isVisibleToPublic);
        if (direct.isPresent()) return direct;

        return slugHistoryRepo.findByOldSlug(slug)
                .flatMap(history -> postRepo.findByPostIdAndDeletedAtIsNull(history.getPostId()))
                .filter(PostJpa::isVisibleToPublic);
    }

    @Transactional(readOnly = true)
    public List<PostDtos.SummaryDto> related(String slug) {
        PostJpa post = findVisibleEntity(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));

        List<Integer> tagIds = post.getTags().stream().map(PostTagJpa::getTagId).toList();
        Integer categoryId = post.getCategory() == null ? null : post.getCategory().getCategoryId();

        // JPQL không nhận danh sách rỗng trong mệnh đề IN của MySQL, nên đưa vào
        // một giá trị không bao giờ khớp và tắt nhánh tag bằng cờ hasTags.
        List<Integer> safeTagIds = tagIds.isEmpty() ? List.of(-1) : tagIds;

        return postRepo.findRelated(
                        post.getPostId(),
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
    public List<PostDtos.CategoryDto> activeCategories() {
        return categoryRepo.findByIsActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(category -> mapper.toCategoryDto(category, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostDtos.TagDto> popularTags(int limit) {
        return tagRepo.findByOrderByUsageCountDescNameAsc(PageRequest.of(0, Math.max(1, Math.min(limit, 100))))
                .stream()
                .map(mapper::toTagDto)
                .toList();
    }

    @Transactional
    public void registerShare(String slug) {
        findVisibleEntity(slug).ifPresent(post -> postRepo.incrementShareCount(post.getPostId()));
    }

    private void requireVisible(PostJpa post) {
        if (!post.isVisibleToPublic()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết");
        }
    }
}
