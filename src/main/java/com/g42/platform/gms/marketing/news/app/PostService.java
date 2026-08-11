package com.g42.platform.gms.marketing.news.app;

import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.domain.PostCtaPosition;
import com.g42.platform.gms.marketing.news.domain.PostStatus;
import com.g42.platform.gms.marketing.news.infrastructure.entity.*;
import com.g42.platform.gms.marketing.news.infrastructure.repository.*;
import com.g42.platform.gms.marketing.news.infrastructure.specification.PostSpecification;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Nghiệp vụ quản trị bài viết: soạn, sửa, duyệt, hẹn giờ, xoá mềm. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final int EXCERPT_MAX_LENGTH = 300;

    private final PostJpaRepo postRepo;
    private final PostCategoryJpaRepo categoryRepo;
    private final PostTagJpaRepo tagRepo;
    private final PostSlugHistoryJpaRepo slugHistoryRepo;
    private final StaffProileJpaRepo staffRepo;
    private final PostContentService contentService;
    private final PostMapper mapper;

    // ---------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public Page<PostDtos.SummaryDto> search(PostStatus status,
                                            Integer categoryId,
                                            String tagSlug,
                                            String keyword,
                                            Pageable pageable) {
        Specification<PostJpa> spec = PostSpecification.notDeleted()
                .and(PostSpecification.hasStatus(status))
                .and(PostSpecification.hasCategoryId(categoryId))
                .and(PostSpecification.hasTagSlug(tagSlug))
                .and(PostSpecification.keyword(keyword));
        return postRepo.findAll(spec, pageable).map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public PostDtos.AdminDetailDto getForEdit(Long postId) {
        return mapper.toAdminDetail(loadOrThrow(postId));
    }

    @Transactional(readOnly = true)
    public PostDtos.StatsDto stats() {
        return new PostDtos.StatsDto(
                postRepo.countByStatusAndDeletedAtIsNull(PostStatus.DRAFT),
                postRepo.countByStatusAndDeletedAtIsNull(PostStatus.PENDING),
                postRepo.countByStatusAndDeletedAtIsNull(PostStatus.SCHEDULED),
                postRepo.countByStatusAndDeletedAtIsNull(PostStatus.PUBLISHED),
                postRepo.countByStatusAndDeletedAtIsNull(PostStatus.ARCHIVED),
                postRepo.sumViewCount()
        );
    }

    // ---------------------------------------------------------------- ghi

    @Transactional
    public PostDtos.AdminDetailDto create(PostDtos.SaveRequest request, Integer authorStaffId) {
        PostJpa post = new PostJpa();
        post.setStatus(PostStatus.DRAFT);
        post.setViewCount(0L);
        post.setShareCount(0L);
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(resolveStaff(authorStaffId));
        post.setSlug(SlugGenerator.toUniqueSlug(
                request.slug(), request.title(), this::isSlugTaken));

        applyEditableFields(post, request);
        return mapper.toAdminDetail(postRepo.save(post));
    }

    /**
     * Sửa bài. Đường dẫn chỉ đổi khi biên tập **chủ động** gõ slug khác — sửa
     * tiêu đề không được kéo theo đổi URL.
     *
     * <p>Bài đã đăng thì đường dẫn là tài sản: nó đã nằm trong link chia sẻ, đã
     * được Google lập chỉ mục. Tự sinh lại slug mỗi lần đổi tiêu đề sẽ liên tục
     * sinh chuyển hướng và làm loãng thứ hạng, nên slug ở đây được coi là cố định
     * cho tới khi có yêu cầu đổi rõ ràng.
     */
    @Transactional
    public PostDtos.AdminDetailDto update(Long postId, PostDtos.SaveRequest request) {
        PostJpa post = loadOrThrow(postId);

        String desiredSlug = SlugGenerator.toSlug(request.slug());
        if (!desiredSlug.isEmpty() && !desiredSlug.equals(post.getSlug())) {
            rememberOldSlug(post);
            post.setSlug(SlugGenerator.toUniqueSlug(desiredSlug, post.getSlug(), this::isSlugTaken));
        }

        applyEditableFields(post, request);
        return mapper.toAdminDetail(postRepo.save(post));
    }

    /**
     * Đổi trạng thái bài. Đây là nơi duy nhất đặt {@code publishedAt} để mọi
     * đường vào (duyệt tay, hẹn giờ, job nền) đều cho ra dữ liệu nhất quán.
     */
    @Transactional
    public PostDtos.AdminDetailDto changeStatus(Long postId,
                                                PostDtos.StatusChangeRequest request,
                                                Integer actorStaffId) {
        PostJpa post = loadOrThrow(postId);
        PostStatus target = request.status();
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu trạng thái cần chuyển");
        }

        switch (target) {
            case PUBLISHED -> {
                post.setStatus(PostStatus.PUBLISHED);
                if (post.getPublishedAt() == null) post.setPublishedAt(LocalDateTime.now());
                post.setScheduledAt(null);
                post.setReviewer(resolveStaff(actorStaffId));
                post.setReviewNote(null);
            }
            case SCHEDULED -> {
                LocalDateTime moment = request.scheduledAt() != null ? request.scheduledAt() : post.getScheduledAt();
                if (moment == null || moment.isBefore(LocalDateTime.now())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Thời điểm hẹn đăng phải nằm ở tương lai");
                }
                post.setStatus(PostStatus.SCHEDULED);
                post.setScheduledAt(moment);
                post.setPublishedAt(null);
                post.setReviewer(resolveStaff(actorStaffId));
            }
            case PENDING -> {
                post.setStatus(PostStatus.PENDING);
                post.setReviewNote(null);
            }
            case DRAFT -> {
                // Trả bài về nháp — ghi lý do để tác giả biết cần sửa gì.
                post.setStatus(PostStatus.DRAFT);
                post.setReviewer(resolveStaff(actorStaffId));
                post.setReviewNote(request.reviewNote());
            }
            case ARCHIVED -> post.setStatus(PostStatus.ARCHIVED);
        }

        post.setUpdatedAt(LocalDateTime.now());
        return mapper.toAdminDetail(postRepo.save(post));
    }

    @Transactional
    public void softDelete(Long postId) {
        PostJpa post = loadOrThrow(postId);
        post.setDeletedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        postRepo.save(post);
    }

    // ------------------------------------------------------------ nội bộ

    private void applyEditableFields(PostJpa post, PostDtos.SaveRequest request) {
        String safeHtml = contentService.sanitize(request.contentHtml());

        post.setTitle(request.title().trim());
        post.setContentHtml(safeHtml);
        post.setExcerpt(firstNonBlank(request.excerpt(),
                contentService.buildExcerpt(safeHtml, EXCERPT_MAX_LENGTH)));
        post.setThumbnailUrl(firstNonBlank(request.thumbnailUrl(), contentService.firstImageUrl(safeHtml)));
        post.setCoverUrl(request.coverUrl());
        post.setIsFeatured(Boolean.TRUE.equals(request.isFeatured()));
        post.setPinnedOrder(request.pinnedOrder());
        post.setScheduledAt(request.scheduledAt());
        post.setSeoTitle(request.seoTitle());
        post.setSeoDescription(request.seoDescription());
        post.setSeoKeywords(request.seoKeywords());
        // Ảnh chia sẻ phải luôn có, nếu không thẻ Facebook sẽ trống trơn.
        post.setOgImageUrl(firstNonBlank(request.ogImageUrl(), request.coverUrl(), post.getThumbnailUrl()));
        post.setCanonicalUrl(request.canonicalUrl());
        post.setAllowIndex(request.allowIndex() == null || request.allowIndex());
        post.setReadingMinutes(contentService.estimateReadingMinutes(safeHtml));
        post.setUpdatedAt(LocalDateTime.now());

        post.setCategory(resolveCategory(request.categoryId()));
        applyTags(post, request.tags());
        applyCtas(post, request.ctas());
    }

    private void applyTags(PostJpa post, List<String> tagNames) {
        Set<PostTagJpa> resolved = new LinkedHashSet<>();
        if (tagNames != null) {
            for (String rawName : tagNames) {
                if (rawName == null || rawName.isBlank()) continue;
                String name = rawName.trim();
                String slug = SlugGenerator.toSlug(name);
                if (slug.isEmpty()) continue;

                PostTagJpa tag = tagRepo.findBySlug(slug).orElseGet(() -> {
                    PostTagJpa created = new PostTagJpa();
                    created.setName(name);
                    created.setSlug(slug);
                    created.setUsageCount(0);
                    created.setCreatedAt(LocalDateTime.now());
                    return tagRepo.save(created);
                });
                resolved.add(tag);
            }
        }

        Set<PostTagJpa> previous = new LinkedHashSet<>(post.getTags());
        post.getTags().clear();
        post.getTags().addAll(resolved);

        // So khớp bằng khoá chứ không bằng tham chiếu: PostTagJpa không định nghĩa
        // equals nên tag vừa tạo và tag đọc lại từ kho sẽ không bao giờ "bằng nhau".
        Set<Integer> previousIds = previous.stream().map(PostTagJpa::getTagId).collect(Collectors.toSet());
        Set<Integer> resolvedIds = resolved.stream().map(PostTagJpa::getTagId).collect(Collectors.toSet());

        // Giữ usage_count khớp thực tế để trang tag phổ biến không xếp sai.
        previous.stream().filter(tag -> !resolvedIds.contains(tag.getTagId())).forEach(tag -> adjustUsage(tag, -1));
        resolved.stream().filter(tag -> !previousIds.contains(tag.getTagId())).forEach(tag -> adjustUsage(tag, 1));
    }

    private void adjustUsage(PostTagJpa tag, int delta) {
        int next = (tag.getUsageCount() == null ? 0 : tag.getUsageCount()) + delta;
        tag.setUsageCount(Math.max(0, next));
        tagRepo.save(tag);
    }

    private void applyCtas(PostJpa post, List<PostDtos.CtaDto> ctas) {
        post.getCtas().clear();
        if (ctas == null) return;

        List<PostCtaJpa> rebuilt = new ArrayList<>();
        int order = 0;
        for (PostDtos.CtaDto dto : ctas) {
            if (dto == null || dto.ctaType() == null) continue;
            PostCtaJpa cta = new PostCtaJpa();
            cta.setPost(post);
            cta.setCtaType(dto.ctaType());
            cta.setLabel(dto.label());
            cta.setSubLabel(dto.subLabel());
            cta.setTargetUrl(dto.targetUrl());
            cta.setCatalogItemId(dto.catalogItemId());
            cta.setPosition(dto.position() == null ? PostCtaPosition.BOTTOM : dto.position());
            cta.setDisplayOrder(dto.displayOrder() == null ? order : dto.displayOrder());
            cta.setIsActive(dto.isActive() == null || dto.isActive());
            rebuilt.add(cta);
            order++;
        }
        post.getCtas().addAll(rebuilt);
    }

    /** Ghi lại slug cũ để link đã chia sẻ vẫn chuyển hướng đúng bài. */
    private void rememberOldSlug(PostJpa post) {
        if (post.getSlug() == null || post.getSlug().isBlank()) return;
        if (slugHistoryRepo.existsByOldSlug(post.getSlug())) return;

        PostSlugHistoryJpa history = new PostSlugHistoryJpa();
        history.setPostId(post.getPostId());
        history.setOldSlug(post.getSlug());
        history.setCreatedAt(LocalDateTime.now());
        slugHistoryRepo.save(history);
    }

    /**
     * Slug đã bị chiếm nếu đang thuộc một bài khác hoặc từng là slug cũ của bài
     * nào đó — tái dùng slug cũ sẽ làm chuyển hướng trỏ sai bài.
     */
    private boolean isSlugTaken(String slug) {
        return postRepo.existsBySlug(slug) || slugHistoryRepo.existsByOldSlug(slug);
    }

    private PostCategoryJpa resolveCategory(Integer categoryId) {
        if (categoryId == null) return null;
        return categoryRepo.findById(categoryId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại"));
    }

    private StaffProfileJpa resolveStaff(Integer staffId) {
        if (staffId == null) return null;
        return staffRepo.findById(staffId).orElse(null);
    }

    private PostJpa loadOrThrow(Long postId) {
        return postRepo.findByPostIdAndDeletedAtIsNull(postId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
