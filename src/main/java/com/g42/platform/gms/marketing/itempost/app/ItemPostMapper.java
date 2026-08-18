package com.g42.platform.gms.marketing.itempost.app;

import com.g42.platform.gms.marketing.itempost.api.dto.ItemPostDtos;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostCategoryJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostCtaJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostTagJpa;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Chuyển thực thể sang DTO. Tách riêng để controller và service không tự nắn dữ liệu. */
@Component
public class ItemPostMapper {

    public ItemPostDtos.TagDto toTagDto(ItemPostTagJpa tag) {
        return new ItemPostDtos.TagDto(tag.getTagId(), tag.getName(), tag.getSlug(), tag.getUsageCount());
    }

    public ItemPostDtos.CategoryDto toCategoryDto(ItemPostCategoryJpa category, Long postCount) {
        return new ItemPostDtos.CategoryDto(
                category.getCategoryId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getIsActive(),
                category.getSeoTitle(),
                category.getSeoDescription(),
                postCount
        );
    }

    public ItemPostDtos.CtaDto toCtaDto(ItemPostCtaJpa cta) {
        return new ItemPostDtos.CtaDto(
                cta.getCtaId(),
                cta.getCtaType(),
                cta.getLabel(),
                cta.getSubLabel(),
                cta.getTargetUrl(),
                cta.getCatalogItemId(),
                cta.getPosition(),
                cta.getDisplayOrder(),
                cta.getIsActive()
        );
    }

    public ItemPostDtos.SummaryDto toSummary(ItemPostJpa post) {
        return new ItemPostDtos.SummaryDto(
                post.getItemPostId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getThumbnailUrl(),
                post.getCatalogItemId(),
                categoryName(post.getCategory()),
                post.getCategory() == null ? null : post.getCategory().getSlug(),
                post.getStatus(),
                post.getIsFeatured(),
                post.getPinnedOrder(),
                post.getPublishedAt(),
                post.getScheduledAt(),
                staffName(post.getAuthor()),
                post.getViewCount(),
                post.getReadingMinutes(),
                toTagDtos(post.getTags())
        );
    }

    public ItemPostDtos.DetailDto toDetail(ItemPostJpa post, String redirectSlug) {
        StaffProfileJpa author = post.getAuthor();
        return new ItemPostDtos.DetailDto(
                post.getItemPostId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getContentHtml(),
                post.getThumbnailUrl(),
                post.getCoverUrl(),
                post.getCatalogItemId(),
                categoryName(post.getCategory()),
                post.getCategory() == null ? null : post.getCategory().getSlug(),
                post.getPublishedAt(),
                post.getUpdatedAt(),
                staffName(author),
                author == null ? null : author.getAvatar(),
                author == null ? null : author.getPosition(),
                post.getViewCount(),
                post.getShareCount(),
                post.getReadingMinutes(),
                post.getSeoTitle(),
                post.getSeoDescription(),
                post.getSeoKeywords(),
                post.getOgImageUrl(),
                post.getCanonicalUrl(),
                post.getAllowIndex(),
                toTagDtos(post.getTags()),
                toCtaDtos(post),
                redirectSlug
        );
    }

    public ItemPostDtos.AdminDetailDto toAdminDetail(ItemPostJpa post) {
        return new ItemPostDtos.AdminDetailDto(
                post.getItemPostId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getContentHtml(),
                post.getThumbnailUrl(),
                post.getCoverUrl(),
                post.getCatalogItemId(),
                post.getCategory() == null ? null : post.getCategory().getCategoryId(),
                categoryName(post.getCategory()),
                post.getStatus(),
                post.getIsFeatured(),
                post.getPinnedOrder(),
                post.getPublishedAt(),
                post.getScheduledAt(),
                post.getAuthor() == null ? null : post.getAuthor().getStaffId(),
                staffName(post.getAuthor()),
                post.getReviewer() == null ? null : post.getReviewer().getStaffId(),
                staffName(post.getReviewer()),
                post.getReviewNote(),
                post.getViewCount(),
                post.getShareCount(),
                post.getReadingMinutes(),
                post.getSeoTitle(),
                post.getSeoDescription(),
                post.getSeoKeywords(),
                post.getOgImageUrl(),
                post.getCanonicalUrl(),
                post.getAllowIndex(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                toTagDtos(post.getTags()),
                toCtaDtos(post)
        );
    }

    private List<ItemPostDtos.TagDto> toTagDtos(Set<ItemPostTagJpa> tags) {
        if (tags == null) return List.of();
        return tags.stream()
                .sorted(Comparator.comparing(ItemPostTagJpa::getName, Comparator.nullsLast(String::compareTo)))
                .map(this::toTagDto)
                .toList();
    }

    private List<ItemPostDtos.CtaDto> toCtaDtos(ItemPostJpa post) {
        if (post.getCtas() == null) return List.of();
        return post.getCtas().stream()
                .filter(cta -> !Boolean.FALSE.equals(cta.getIsActive()))
                .map(this::toCtaDto)
                .toList();
    }

    private String categoryName(ItemPostCategoryJpa category) {
        return category == null ? null : category.getName();
    }

    private String staffName(StaffProfileJpa staff) {
        return staff == null ? null : staff.getFullName();
    }
}
