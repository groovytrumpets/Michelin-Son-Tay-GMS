package com.g42.platform.gms.marketing.news.app;

import com.g42.platform.gms.marketing.news.infrastructure.entity.PostCategoryJpa;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostJpa;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostTagJpa;
import com.g42.platform.gms.marketing.news.infrastructure.repository.PostCategoryJpaRepo;
import com.g42.platform.gms.marketing.news.infrastructure.repository.PostJpaRepo;
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
 * Sinh nội dung cho máy đọc: HTML prerender, sitemap và RSS.
 *
 * <p>Frontend là ứng dụng một trang không dựng sẵn HTML, nên bot của Facebook,
 * Zalo và Twitter tải trang về chỉ thấy khung rỗng — link chia sẻ vì thế không
 * hiện được tiêu đề hay ảnh. Nginx nhận diện các bot này rồi chuyển hướng sang
 * {@link #renderPostHtml} để chúng nhận HTML đầy đủ thẻ meta, còn người dùng
 * thật vẫn nhận ứng dụng React như thường.
 */
@Service
@RequiredArgsConstructor
public class PostSeoService {

    private static final int SITEMAP_LIMIT = 5000;
    private static final int RSS_LIMIT = 30;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter RSS_DATE = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final PostJpaRepo postRepo;
    private final PostCategoryJpaRepo categoryRepo;
    private final PostContentService contentService;

    /** Tên miền công khai của trang khách; dùng để dựng URL tuyệt đối. */
    @Value("${news.site.url:https://sontaygarage.vn}")
    private String siteUrl;

    @Value("${news.site.name:Michelin Sơn Tây}")
    private String siteName;

    @Value("${news.site.logo:https://sontaygarage.vn/Copy%20of%20Logo.png}")
    private String siteLogo;

    /** Tên miền của chính backend — nơi đặt sitemap và RSS khai báo với Google. */
    @Value("${news.api.url:https://api.sontaygarage.vn}")
    private String apiUrl;

    // ------------------------------------------------------------- prerender

    /**
     * HTML tĩnh của một bài viết dành cho bot. Ngoài thẻ meta còn kèm cả nội
     * dung bài để Google đọc được ngay mà không phải chạy JavaScript.
     */
    @Transactional(readOnly = true)
    public String renderPostHtml(PostJpa post) {
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
        String categoryName = post.getCategory() == null ? "Tin tức" : post.getCategory().getName();
        String keywords = firstNonBlank(
                post.getSeoKeywords(),
                post.getTags().stream().map(PostTagJpa::getName).collect(Collectors.joining(", ")));

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
                <p><a href="%s/tin-tuc">Quay lại trang tin tức</a></p>
                </body></html>
                """.formatted(escape(siteName), escape(siteUrl()));
    }

    // --------------------------------------------------------------- sitemap

    @Transactional(readOnly = true)
    public String sitemapIndex() {
        String now = toIso(LocalDateTime.now());
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                <sitemap><loc>%s/seo/sitemap-posts.xml</loc><lastmod>%s</lastmod></sitemap>
                <sitemap><loc>%s/seo/news-sitemap.xml</loc><lastmod>%s</lastmod></sitemap>
                </sitemapindex>
                """.formatted(apiBase(), now, apiBase(), now);
    }

    @Transactional(readOnly = true)
    public String postsSitemap() {
        List<PostJpa> posts = postRepo.findIndexablePublished(LocalDateTime.now(), PageRequest.of(0, SITEMAP_LIMIT));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        xml.append(urlEntry(siteUrl() + "/tin-tuc", toIso(LocalDateTime.now()), "daily", "0.9"));
        for (PostCategoryJpa category : categoryRepo.findByIsActiveTrueOrderByDisplayOrderAscNameAsc()) {
            xml.append(urlEntry(siteUrl() + "/tin-tuc/danh-muc/" + category.getSlug(),
                    toIso(LocalDateTime.now()), "weekly", "0.7"));
        }
        for (PostJpa post : posts) {
            String lastmod = toIso(post.getUpdatedAt() != null ? post.getUpdatedAt() : post.getPublishedAt());
            xml.append(urlEntry(postUrl(post.getSlug()), lastmod, "monthly", "0.8"));
        }

        return xml.append("</urlset>\n").toString();
    }

    /** Sitemap riêng theo chuẩn Google News cho bài trong 48 giờ. */
    @Transactional(readOnly = true)
    public String newsSitemap() {
        LocalDateTime now = LocalDateTime.now();
        List<PostJpa> posts = postRepo.findRecentForNewsSitemap(now.minusHours(48), now);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" ")
                .append("xmlns:news=\"http://www.google.com/schemas/sitemap-news/0.9\">\n");

        for (PostJpa post : posts) {
            xml.append("<url><loc>").append(escape(postUrl(post.getSlug()))).append("</loc>")
                    .append("<news:news><news:publication>")
                    .append("<news:name>").append(escape(siteName)).append("</news:name>")
                    .append("<news:language>vi</news:language>")
                    .append("</news:publication>")
                    .append("<news:publication_date>").append(toIso(post.getPublishedAt())).append("</news:publication_date>")
                    .append("<news:title>").append(escape(post.getTitle())).append("</news:title>")
                    .append("</news:news></url>\n");
        }

        return xml.append("</urlset>\n").toString();
    }

    @Transactional(readOnly = true)
    public String rssFeed() {
        List<PostJpa> posts = postRepo.findIndexablePublished(LocalDateTime.now(), PageRequest.of(0, RSS_LIMIT));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<rss version=\"2.0\"><channel>\n")
                .append("<title>").append(escape("Tin tức " + siteName)).append("</title>\n")
                .append("<link>").append(escape(siteUrl() + "/tin-tuc")).append("</link>\n")
                .append("<description>").append(escape("Tin khuyến mãi, kiến thức chăm xe và hoạt động của " + siteName))
                .append("</description>\n")
                .append("<language>vi</language>\n");

        for (PostJpa post : posts) {
            xml.append("<item>")
                    .append("<title>").append(escape(post.getTitle())).append("</title>")
                    .append("<link>").append(escape(postUrl(post.getSlug()))).append("</link>")
                    .append("<guid isPermaLink=\"true\">").append(escape(postUrl(post.getSlug()))).append("</guid>")
                    .append("<description>").append(escape(firstNonBlank(post.getExcerpt(), ""))).append("</description>")
                    .append("<pubDate>").append(toRssDate(post.getPublishedAt())).append("</pubDate>")
                    .append("</item>\n");
        }

        return xml.append("</channel></rss>\n").toString();
    }

    public String robotsTxt() {
        return """
                User-agent: *
                Allow: /
                Disallow: /login
                Disallow: /checkout
                Disallow: /user-profile

                Sitemap: %s/seo/sitemap.xml
                """.formatted(apiBase());
    }

    // ---------------------------------------------------------------- nội bộ

    private String articleJsonLd(String url, String title, String description,
                                 String image, String published, String modified, String author) {
        return """
                {"@context":"https://schema.org","@type":"NewsArticle",\
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

    private String breadcrumbJsonLd(PostJpa post, String url) {
        String categoryName = post.getCategory() == null ? "Tin tức" : post.getCategory().getName();
        String categoryUrl = post.getCategory() == null
                ? siteUrl() + "/tin-tuc"
                : siteUrl() + "/tin-tuc/danh-muc/" + post.getCategory().getSlug();

        return """
                {"@context":"https://schema.org","@type":"BreadcrumbList","itemListElement":[\
                {"@type":"ListItem","position":1,"name":"Trang chủ","item":"%s"},\
                {"@type":"ListItem","position":2,"name":"Tin tức","item":"%s/tin-tuc"},\
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
        return siteUrl() + "/tin-tuc/" + slug;
    }

    private String siteUrl() {
        return siteUrl == null ? "" : siteUrl.replaceAll("/+$", "");
    }

    private String apiBase() {
        return apiUrl == null ? "" : apiUrl.replaceAll("/+$", "");
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

    private String toRssDate(LocalDateTime moment) {
        LocalDateTime value = moment == null ? LocalDateTime.now() : moment;
        return ZonedDateTime.of(value, ZONE).format(RSS_DATE);
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
