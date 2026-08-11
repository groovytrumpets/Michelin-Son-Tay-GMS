package com.g42.platform.gms.marketing.news.app;

import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostCategoryJpa;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostCtaJpa;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostJpa;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostTagJpa;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Chuyển thực thể sang DTO. Tách riêng để controller và service không tự nắn dữ liệu. */
@Component
public class PostMapper {

    public PostDtos.TagDto toTagDto(PostTagJpa tag) {
        return new PostDtos.TagDto(tag.getTagId(), tag.getName(), tag.getSlug(), tag.getUsageCount());
    }

    public PostDtos.CategoryDto toCategoryDto(PostCategoryJpa category, Long postCount) {
        return new PostDtos.CategoryDto(
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

    public PostDtos.CtaDto toCtaDto(PostCtaJpa cta) {
        return new PostDtos.CtaDto(
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

    public PostDtos.SummaryDto toSummary(PostJpa post) {
        return new PostDtos.SummaryDto(
                post.getPostId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getThumbnailUrl(),
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

    public PostDtos.DetailDto toDetail(PostJpa post, String redirectSlug) {
        StaffProfileJpa author = post.getAuthor();
        return new PostDtos.DetailDto(
                post.getPostId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getContentHtml(),
                post.getThumbnailUrl(),
                post.getCoverUrl(),
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

    public PostDtos.AdminDetailDto toAdminDetail(PostJpa post) {
        return new PostDtos.AdminDetailDto(
                post.getPostId(),
                post.getSlug(),
                post.getTitle(),
                post.getExcerpt(),
                post.getContentHtml(),
                post.getThumbnailUrl(),
                post.getCoverUrl(),
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

    private List<PostDtos.TagDto> toTagDtos(Set<PostTagJpa> tags) {
        if (tags == null) return List.of();
        return tags.stream()
                .sorted(Comparator.comparing(PostTagJpa::getName, Comparator.nullsLast(String::compareTo)))
                .map(this::toTagDto)
                .toList();
    }

    private List<PostDtos.CtaDto> toCtaDtos(PostJpa post) {
        if (post.getCtas() == null) return List.of();
        return post.getCtas().stream()
                .filter(cta -> !Boolean.FALSE.equals(cta.getIsActive()))
                .map(this::toCtaDto)
                .toList();
    }

    private String categoryName(PostCategoryJpa category) {
        return category == null ? null : category.getName();
    }

    private String staffName(StaffProfileJpa staff) {
        return staff == null ? null : staff.getFullName();
    }
}
