package com.g42.platform.gms.marketing.itempost.app;

import com.g42.platform.gms.marketing.itempost.api.dto.ItemPostDtos;
import com.g42.platform.gms.marketing.itempost.domain.ItemPostCtaPosition;
import com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.*;
import com.g42.platform.gms.marketing.itempost.infrastructure.repository.*;
import com.g42.platform.gms.marketing.itempost.infrastructure.specification.ItemPostSpecification;
import com.g42.platform.gms.marketing.news.app.SlugGenerator;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.repository.ServiceJpaRepository;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Nghiệp vụ quản trị bài viết phụ tùng: soạn, sửa, duyệt, hẹn giờ, xoá mềm. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemPostService {

    private static final int EXCERPT_MAX_LENGTH = 300;

    private final ItemPostJpaRepo itemPostRepo;
    private final ItemPostCategoryJpaRepo categoryRepo;
    private final ItemPostTagJpaRepo tagRepo;
    private final ItemPostSlugHistoryJpaRepo slugHistoryRepo;
    private final StaffProileJpaRepo staffRepo;
    private final CatalogItemJpaRepo catalogItemRepo;
    private final ServiceJpaRepository serviceJpaRepo;
    private final ItemPostContentService contentService;
    private final ItemPostMapper mapper;

    /** Loại catalog item công khai trên storefront — dùng để phạm vi hoá backfill. */
    private static final Set<CatalogItemType> BACKFILLABLE_TYPES = Set.of(
            CatalogItemType.SERVICE, CatalogItemType.PART, CatalogItemType.EQUIPMENT,
            CatalogItemType.MACHINERY, CatalogItemType.COMBO);

    // ---------------------------------------------------------------- đọc

    @Transactional(readOnly = true)
    public Page<ItemPostDtos.SummaryDto> search(ItemPostStatus status,
                                            Integer categoryId,
                                            String tagSlug,
                                            Integer catalogItemId,
                                            String keyword,
                                            Pageable pageable) {
        Specification<ItemPostJpa> spec = ItemPostSpecification.notDeleted()
                .and(ItemPostSpecification.hasStatus(status))
                .and(ItemPostSpecification.hasCategoryId(categoryId))
                .and(ItemPostSpecification.hasTagSlug(tagSlug))
                .and(ItemPostSpecification.hasCatalogItemId(catalogItemId))
                .and(ItemPostSpecification.keyword(keyword));
        return itemPostRepo.findAll(spec, pageable).map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ItemPostDtos.AdminDetailDto getForEdit(Long itemPostId) {
        return mapper.toAdminDetail(loadOrThrow(itemPostId));
    }

    /** Bài viết phụ tùng gắn với một mặt hàng — dùng cho điểm vào từ /warehouse-management. */
    @Transactional(readOnly = true)
    public List<ItemPostDtos.AdminDetailDto> getByCatalogItem(Integer catalogItemId) {
        return itemPostRepo.findByCatalogItemIdAndDeletedAtIsNull(catalogItemId).stream()
                .map(mapper::toAdminDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemPostDtos.StatsDto stats() {
        return new ItemPostDtos.StatsDto(
                itemPostRepo.countByStatusAndDeletedAtIsNull(ItemPostStatus.DRAFT),
                itemPostRepo.countByStatusAndDeletedAtIsNull(ItemPostStatus.PENDING),
                itemPostRepo.countByStatusAndDeletedAtIsNull(ItemPostStatus.SCHEDULED),
                itemPostRepo.countByStatusAndDeletedAtIsNull(ItemPostStatus.PUBLISHED),
                itemPostRepo.countByStatusAndDeletedAtIsNull(ItemPostStatus.ARCHIVED),
                itemPostRepo.sumViewCount()
        );
    }

    // ---------------------------------------------------------------- ghi

    @Transactional
    public ItemPostDtos.AdminDetailDto create(ItemPostDtos.SaveRequest request, Integer authorStaffId) {
        requireCatalogItemExists(request.catalogItemId());

        ItemPostJpa post = new ItemPostJpa();
        post.setStatus(ItemPostStatus.DRAFT);
        post.setViewCount(0L);
        post.setShareCount(0L);
        post.setCreatedAt(LocalDateTime.now());
        post.setAuthor(resolveStaff(authorStaffId));
        post.setCatalogItemId(request.catalogItemId());
        post.setSlug(SlugGenerator.toUniqueSlug(
                request.slug(), request.title(), this::isSlugTaken));

        applyEditableFields(post, request);
        return mapper.toAdminDetail(itemPostRepo.save(post));
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
    public ItemPostDtos.AdminDetailDto update(Long itemPostId, ItemPostDtos.SaveRequest request) {
        ItemPostJpa post = loadOrThrow(itemPostId);
        requireCatalogItemExists(request.catalogItemId());

        String desiredSlug = SlugGenerator.toSlug(request.slug());
        if (!desiredSlug.isEmpty() && !desiredSlug.equals(post.getSlug())) {
            rememberOldSlug(post);
            post.setSlug(SlugGenerator.toUniqueSlug(desiredSlug, post.getSlug(), this::isSlugTaken));
        }

        post.setCatalogItemId(request.catalogItemId());
        applyEditableFields(post, request);
        return mapper.toAdminDetail(itemPostRepo.save(post));
    }

    /**
     * Đổi trạng thái bài. Đây là nơi duy nhất đặt {@code publishedAt} để mọi
     * đường vào (duyệt tay, hẹn giờ, job nền) đều cho ra dữ liệu nhất quán.
     */
    @Transactional
    public ItemPostDtos.AdminDetailDto changeStatus(Long itemPostId,
                                                ItemPostDtos.StatusChangeRequest request,
                                                Integer actorStaffId) {
        ItemPostJpa post = loadOrThrow(itemPostId);
        ItemPostStatus target = request.status();
        if (target == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu trạng thái cần chuyển");
        }

        switch (target) {
            case PUBLISHED -> {
                post.setStatus(ItemPostStatus.PUBLISHED);
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
                post.setStatus(ItemPostStatus.SCHEDULED);
                post.setScheduledAt(moment);
                post.setPublishedAt(null);
                post.setReviewer(resolveStaff(actorStaffId));
            }
            case PENDING -> {
                post.setStatus(ItemPostStatus.PENDING);
                post.setReviewNote(null);
            }
            case DRAFT -> {
                // Trả bài về nháp — ghi lý do để tác giả biết cần sửa gì.
                post.setStatus(ItemPostStatus.DRAFT);
                post.setReviewer(resolveStaff(actorStaffId));
                post.setReviewNote(request.reviewNote());
            }
            case ARCHIVED -> post.setStatus(ItemPostStatus.ARCHIVED);
        }

        post.setUpdatedAt(LocalDateTime.now());
        return mapper.toAdminDetail(itemPostRepo.save(post));
    }

    /** Xoá mềm bài viết phụ tùng — đáp ứng yêu cầu bổ sung cơ chế xoá bài cho phụ tùng. */
    @Transactional
    public void delete(Long itemPostId) {
        ItemPostJpa post = loadOrThrow(itemPostId);
        post.setDeletedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        itemPostRepo.save(post);
    }

    /**
     * Tạo item_post PUBLISHED cho mọi catalog item công khai (SERVICE/PART/EQUIPMENT/
     * MACHINERY/COMBO) chưa có bài viết nào — để URL id số cũ có slug tương ứng ngay
     * sau khi deploy tính năng này, không cần marketing viết lại tay từng cái.
     *
     * <p>Idempotent: catalog item nào đã có item_post (kể cả không PUBLISHED) thì bị
     * bỏ qua ở lần gọi sau, nên gọi lại nhiều lần là an toàn.
     */
    @Transactional
    public ItemPostDtos.BackfillResultDto backfillFromCatalogItems() {
        Set<Integer> catalogItemIdsWithPost = new HashSet<>(itemPostRepo.findDistinctCatalogItemIdsWithPost());

        List<CatalogItemJpa> eligible = catalogItemRepo.findAll().stream()
                .filter(item -> BACKFILLABLE_TYPES.contains(item.getItemType()))
                .toList();
        List<CatalogItemJpa> candidates = eligible.stream()
                .filter(item -> !catalogItemIdsWithPost.contains(item.getItemId()))
                .toList();

        int created = 0;
        for (CatalogItemJpa item : candidates) {
            createPublishedPostFromCatalogItem(item);
            created++;
        }
        return new ItemPostDtos.BackfillResultDto(created, eligible.size() - candidates.size());
    }

    private void createPublishedPostFromCatalogItem(CatalogItemJpa item) {
        ServiceJpaEntity linkedService = item.getServiceId() == null
                ? null
                : serviceJpaRepo.findById(item.getServiceId()).orElse(null);

        String title = firstNonBlank(
                linkedService == null ? null : linkedService.getTitle(),
                item.getItemName());
        String contentHtml = firstNonBlank(
                linkedService == null ? null : linkedService.getFullDescription(),
                item.getDescription());

        ItemPostDtos.SaveRequest request = new ItemPostDtos.SaveRequest(
                title,
                null, // slug: để trống, SlugGenerator tự sinh từ title
                item.getItemId(),
                null, // excerpt: tự rút từ contentHtml
                contentHtml,
                item.getImageUrl(),
                null, // coverUrl
                null, // categoryId
                null, // isFeatured
                null, // pinnedOrder
                null, // scheduledAt
                null, // seoTitle
                null, // seoDescription
                null, // seoKeywords
                null, // ogImageUrl
                null, // canonicalUrl
                null, // allowIndex
                null, // tags
                null  // ctas
        );

        ItemPostDtos.AdminDetailDto createdPost = create(request, null);
        changeStatus(createdPost.itemPostId(),
                new ItemPostDtos.StatusChangeRequest(ItemPostStatus.PUBLISHED, null, null),
                null);
    }

    // ------------------------------------------------------------ nội bộ

    private void applyEditableFields(ItemPostJpa post, ItemPostDtos.SaveRequest request) {
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

    private void applyTags(ItemPostJpa post, List<String> tagNames) {
        Set<ItemPostTagJpa> resolved = new LinkedHashSet<>();
        if (tagNames != null) {
            for (String rawName : tagNames) {
                if (rawName == null || rawName.isBlank()) continue;
                String name = rawName.trim();
                String slug = SlugGenerator.toSlug(name);
                if (slug.isEmpty()) continue;

                ItemPostTagJpa tag = tagRepo.findBySlug(slug).orElseGet(() -> {
                    ItemPostTagJpa created = new ItemPostTagJpa();
                    created.setName(name);
                    created.setSlug(slug);
                    created.setUsageCount(0);
                    created.setCreatedAt(LocalDateTime.now());
                    return tagRepo.save(created);
                });
                resolved.add(tag);
            }
        }

        Set<ItemPostTagJpa> previous = new LinkedHashSet<>(post.getTags());
        post.getTags().clear();
        post.getTags().addAll(resolved);

        // So khớp bằng khoá chứ không bằng tham chiếu: ItemPostTagJpa không định
        // nghĩa equals nên tag vừa tạo và tag đọc lại từ kho sẽ không bao giờ "bằng nhau".
        Set<Integer> previousIds = previous.stream().map(ItemPostTagJpa::getTagId).collect(Collectors.toSet());
        Set<Integer> resolvedIds = resolved.stream().map(ItemPostTagJpa::getTagId).collect(Collectors.toSet());

        // Giữ usage_count khớp thực tế để trang tag phổ biến không xếp sai.
        previous.stream().filter(tag -> !resolvedIds.contains(tag.getTagId())).forEach(tag -> adjustUsage(tag, -1));
        resolved.stream().filter(tag -> !previousIds.contains(tag.getTagId())).forEach(tag -> adjustUsage(tag, 1));
    }

    private void adjustUsage(ItemPostTagJpa tag, int delta) {
        int next = (tag.getUsageCount() == null ? 0 : tag.getUsageCount()) + delta;
        tag.setUsageCount(Math.max(0, next));
        tagRepo.save(tag);
    }

    private void applyCtas(ItemPostJpa post, List<ItemPostDtos.CtaDto> ctas) {
        post.getCtas().clear();
        if (ctas == null) return;

        List<ItemPostCtaJpa> rebuilt = new ArrayList<>();
        int order = 0;
        for (ItemPostDtos.CtaDto dto : ctas) {
            if (dto == null || dto.ctaType() == null) continue;
            ItemPostCtaJpa cta = new ItemPostCtaJpa();
            cta.setItemPost(post);
            cta.setCtaType(dto.ctaType());
            cta.setLabel(dto.label());
            cta.setSubLabel(dto.subLabel());
            cta.setTargetUrl(dto.targetUrl());
            cta.setCatalogItemId(dto.catalogItemId());
            cta.setPosition(dto.position() == null ? ItemPostCtaPosition.BOTTOM : dto.position());
            cta.setDisplayOrder(dto.displayOrder() == null ? order : dto.displayOrder());
            cta.setIsActive(dto.isActive() == null || dto.isActive());
            rebuilt.add(cta);
            order++;
        }
        post.getCtas().addAll(rebuilt);
    }

    /** Ghi lại slug cũ để link đã chia sẻ vẫn chuyển hướng đúng bài. */
    private void rememberOldSlug(ItemPostJpa post) {
        if (post.getSlug() == null || post.getSlug().isBlank()) return;
        if (slugHistoryRepo.existsByOldSlug(post.getSlug())) return;

        ItemPostSlugHistoryJpa history = new ItemPostSlugHistoryJpa();
        history.setItemPostId(post.getItemPostId());
        history.setOldSlug(post.getSlug());
        history.setCreatedAt(LocalDateTime.now());
        slugHistoryRepo.save(history);
    }

    /**
     * Slug đã bị chiếm nếu đang thuộc một bài khác hoặc từng là slug cũ của bài
     * nào đó — tái dùng slug cũ sẽ làm chuyển hướng trỏ sai bài. Namespace này
     * độc lập với slug của bảng {@code post} (tin tức).
     */
    private boolean isSlugTaken(String slug) {
        return itemPostRepo.existsBySlug(slug) || slugHistoryRepo.existsByOldSlug(slug);
    }

    private ItemPostCategoryJpa resolveCategory(Integer categoryId) {
        if (categoryId == null) return null;
        return categoryRepo.findById(categoryId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại"));
    }

    private StaffProfileJpa resolveStaff(Integer staffId) {
        if (staffId == null) return null;
        return staffRepo.findById(staffId).orElse(null);
    }

    private void requireCatalogItemExists(Integer catalogItemId) {
        if (catalogItemId == null || !catalogItemRepo.existsById(catalogItemId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phụ tùng không tồn tại");
        }
    }

    private ItemPostJpa loadOrThrow(Long itemPostId) {
        return itemPostRepo.findByItemPostIdAndDeletedAtIsNull(itemPostId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }
}
