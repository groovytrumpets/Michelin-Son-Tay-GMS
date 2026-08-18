package com.g42.platform.gms.marketing.itempost.app;

import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostCategoryJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostTagJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.repository.ItemPostCategoryJpaRepo;
import com.g42.platform.gms.marketing.itempost.infrastructure.repository.ItemPostJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sinh nội dung cho máy đọc: HTML prerender và sitemap cho bài viết phụ tùng.
 *
 * <p>Frontend là ứng dụng một trang không dựng sẵn HTML, nên bot của Facebook,
 * Zalo và Twitter tải trang về chỉ thấy khung rỗng — link chia sẻ vì thế không
 * hiện được tiêu đề hay ảnh. Nginx nhận diện các bot này rồi chuyển hướng sang
 * {@link #renderPostHtml} để chúng nhận HTML đầy đủ thẻ meta, còn người dùng
 * thật vẫn nhận ứng dụng React như thường.
 */
@Service
@RequiredArgsConstructor
public class ItemPostSeoService {

    private static final int SITEMAP_LIMIT = 5000;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ItemPostJpaRepo itemPostRepo;
    private final ItemPostCategoryJpaRepo categoryRepo;
    private final ItemPostContentService contentService;

    /** Tên miền công khai của trang khách; dùng để dựng URL tuyệt đối. */
    @Value("${news.site.url:https://sontaygarage.vn}")
    private String siteUrl;

    @Value("${news.site.name:Michelin Sơn Tây}")
    private String siteName;

    @Value("${news.site.logo:https://sontaygarage.vn/Copy%20of%20Logo.png}")
    private String siteLogo;

    /** Tên miền của chính backend — nơi đặt sitemap khai báo với Google. */
    @Value("${news.api.url:https://api.sontaygarage.vn}")
    private String apiUrl;

    // ------------------------------------------------------------- prerender

    /**
     * HTML tĩnh của một bài viết phụ tùng dành cho bot. Ngoài thẻ meta còn kèm
     * cả nội dung bài để Google đọc được ngay mà không phải chạy JavaScript.
     */
    @Transactional(readOnly = true)
    public String renderPostHtml(ItemPostJpa post) {
        String url = postUrl(post.getSlug());
        String title = firstNonBlank(post.getSeoTitle(), post.getTitle());
        String description = firstNonBlank(
                post.getSeoDescription(),
                post.getExcerpt(),
                contentService.buildExcerpt(post.getContentHtml(), 200));
        String image = absolute(firstNonBlank(post.getOgImageUrl(), post.getCoverUrl(), post.getThumbnailUrl(), siteLogo));
        String canonical = firstNonBlank(post.getCanonicalUrl(), url);
        boolean indexable = !Boolean.FALSE.equals(post.getAllowIndex());
        String published = toIso(post.getPublishedAt());
        String modified = toIso(post.getUpdatedAt() != null ? post.getUpdatedAt() : post.getPublishedAt());
        String author = post.getAuthor() == null ? siteName : post.getAuthor().getFullName();
        String categoryName = post.getCategory() == null ? "Phụ tùng" : post.getCategory().getName();
        String keywords = firstNonBlank(
                post.getSeoKeywords(),
                post.getTags().stream().map(ItemPostTagJpa::getName).collect(Collectors.joining(", ")));

        return """
                <!doctype html>
                <html lang="vi">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <meta name="description" content="%s">
                <meta name="keywords" content="%s">
                <meta name="robots" content="%s">
                <link rel="canonical" href="%s">
                <meta property="og:type" content="article">
                <meta property="og:locale" content="vi_VN">
                <meta property="og:site_name" content="%s">
                <meta property="og:title" content="%s">
                <meta property="og:description" content="%s">
                <meta property="og:url" content="%s">
                <meta property="og:image" content="%s">
                <meta property="og:image:width" content="1200">
                <meta property="og:image:height" content="630">
                <meta property="article:published_time" content="%s">
                <meta property="article:modified_time" content="%s">
                <meta property="article:section" content="%s">
                <meta property="article:author" content="%s">
                <meta name="twitter:card" content="summary_large_image">
                <meta name="twitter:title" content="%s">
                <meta name="twitter:description" content="%s">
                <meta name="twitter:image" content="%s">
                <script type="application/ld+json">%s</script>
                <script type="application/ld+json">%s</script>
                </head>
                <body>
                <article>
                <h1>%s</h1>
                <p><em>%s</em></p>
                <p>%s</p>
                %s
                </article>
                <p><a href="%s">Xem bài viết đầy đủ tại %s</a></p>
                </body>
                </html>
                """.formatted(
                escape(title),
                escape(description),
                escape(keywords),
                indexable ? "index, follow, max-image-preview:large" : "noindex, nofollow",
                escape(canonical),
                escape(siteName),
                escape(title),
                escape(description),
                escape(url),
                escape(image),
                published,
                modified,
                escape(categoryName),
                escape(author),
                escape(title),
                escape(description),
                escape(image),
                articleJsonLd(url, title, description, image, published, modified, author),
                breadcrumbJsonLd(post, url),
                escape(post.getTitle()),
                escape(categoryName + " · " + author),
                escape(description),
                post.getContentHtml() == null ? "" : post.getContentHtml(),
                escape(url),
                escape(siteName));
    }

    /** Trang thay thế khi bot mở một slug không tồn tại. */
    public String renderNotFoundHtml() {
        return """
                <!doctype html>
                <html lang="vi"><head><meta charset="utf-8">
                <title>Không tìm thấy bài viết - %s</title>
                <meta name="robots" content="noindex, nofollow">
                </head><body><h1>Không tìm thấy bài viết</h1>
                <p><a href="%s/phu-tung">Quay lại trang phụ tùng</a></p>
                </body></html>
                """.formatted(escape(siteName), escape(siteUrl()));
    }

    // --------------------------------------------------------------- sitemap

    @Transactional(readOnly = true)
    public String itemPostsSitemap() {
        List<ItemPostJpa> posts = itemPostRepo.findIndexablePublished(LocalDateTime.now(), PageRequest.of(0, SITEMAP_LIMIT));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        xml.append(urlEntry(siteUrl() + "/phu-tung", toIso(LocalDateTime.now()), "daily", "0.9"));
        for (ItemPostCategoryJpa category : categoryRepo.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()) {
            xml.append(urlEntry(siteUrl() + "/phu-tung/danh-muc/" + category.getSlug(),
                    toIso(LocalDateTime.now()), "weekly", "0.7"));
        }
        for (ItemPostJpa post : posts) {
            String lastmod = toIso(post.getUpdatedAt() != null ? post.getUpdatedAt() : post.getPublishedAt());
            xml.append(urlEntry(postUrl(post.getSlug()), lastmod, "monthly", "0.8"));
        }

        return xml.append("</urlset>\n").toString();
    }

    // ---------------------------------------------------------------- nội bộ

    private String articleJsonLd(String url, String title, String description,
                                 String image, String published, String modified, String author) {
        return """
                {"@context":"https://schema.org","@type":"Article",\
                "headline":"%s","description":"%s","image":["%s"],\
                "datePublished":"%s","dateModified":"%s",\
                "author":{"@type":"Person","name":"%s"},\
                "publisher":{"@type":"Organization","name":"%s","logo":{"@type":"ImageObject","url":"%s"}},\
                "mainEntityOfPage":{"@type":"WebPage","@id":"%s"},\
                "inLanguage":"vi-VN"}\
                """.formatted(
                json(title), json(description), json(image), published, modified,
                json(author), json(siteName), json(siteLogo), json(url));
    }

    private String breadcrumbJsonLd(ItemPostJpa post, String url) {
        String categoryName = post.getCategory() == null ? "Phụ tùng" : post.getCategory().getName();
        String categoryUrl = post.getCategory() == null
                ? siteUrl() + "/phu-tung"
                : siteUrl() + "/phu-tung/danh-muc/" + post.getCategory().getSlug();

        return """
                {"@context":"https://schema.org","@type":"BreadcrumbList","itemListElement":[\
                {"@type":"ListItem","position":1,"name":"Trang chủ","item":"%s"},\
                {"@type":"ListItem","position":2,"name":"Phụ tùng","item":"%s/phu-tung"},\
                {"@type":"ListItem","position":3,"name":"%s","item":"%s"},\
                {"@type":"ListItem","position":4,"name":"%s","item":"%s"}]}\
                """.formatted(
                json(siteUrl()), json(siteUrl()),
                json(categoryName), json(categoryUrl),
                json(post.getTitle()), json(url));
    }

    private String urlEntry(String loc, String lastmod, String changefreq, String priority) {
        return "<url><loc>" + escape(loc) + "</loc>"
                + "<lastmod>" + lastmod + "</lastmod>"
                + "<changefreq>" + changefreq + "</changefreq>"
                + "<priority>" + priority + "</priority></url>\n";
    }

    public String postUrl(String slug) {
        return siteUrl() + "/phu-tung/" + slug;
    }

    private String siteUrl() {
        return siteUrl == null ? "" : siteUrl.replaceAll("/+$", "");
    }

    private String absolute(String url) {
        if (url == null || url.isBlank()) return siteLogo;
        if (url.startsWith("http://") || url.startsWith("https://")) return url;
        return siteUrl() + (url.startsWith("/") ? url : "/" + url);
    }

    private String toIso(LocalDateTime moment) {
        LocalDateTime value = moment == null ? LocalDateTime.now() : moment;
        return ZonedDateTime.of(value, ZONE).format(ISO_DATE);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    /** Thoát ký tự cho thuộc tính HTML và nội dung XML. */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Thoát ký tự cho chuỗi nằm trong JSON-LD. */
    private static String json(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");
    }
}
