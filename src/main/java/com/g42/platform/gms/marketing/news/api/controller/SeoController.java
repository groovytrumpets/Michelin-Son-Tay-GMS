package com.g42.platform.gms.marketing.news.api.controller;

import com.g42.platform.gms.marketing.news.app.PostSeoService;
import com.g42.platform.gms.marketing.news.app.PublicPostService;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Nội dung dành cho máy đọc.
 *
 * <p>Trang khách là ứng dụng một trang nên bot mạng xã hội không thấy được thẻ
 * meta. Nginx nhận diện User-Agent của bot rồi chuyển tiếp sang
 * {@code /seo/prerender/...} ở đây; cấu hình mẫu nằm trong
 * {@code docs/NEWS_BLOG_DESIGN.md}.
 */
@RestController
@RequestMapping("/seo")
@RequiredArgsConstructor
public class SeoController {

    private static final MediaType HTML = MediaType.valueOf("text/html; charset=UTF-8");
    private static final MediaType XML = MediaType.valueOf("application/xml; charset=UTF-8");
    private static final MediaType TEXT = MediaType.valueOf("text/plain; charset=UTF-8");
    private static final MediaType RSS = MediaType.valueOf("application/rss+xml; charset=UTF-8");

    private final PostSeoService seoService;
    private final PublicPostService publicPostService;

    /** HTML đầy đủ thẻ meta của một bài viết, phục vụ bot chia sẻ và bot tìm kiếm. */
    @GetMapping(value = "/prerender/tin-tuc/{slug}", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> prerenderPost(@PathVariable String slug) {
        Optional<PostJpa> post = publicPostService.findVisibleEntity(slug);
        if (post.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(HTML)
                    .body(seoService.renderNotFoundHtml());
        }
        return ResponseEntity.ok().contentType(HTML).body(seoService.renderPostHtml(post.get()));
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml; charset=UTF-8")
    public ResponseEntity<String> sitemapIndex() {
        return ResponseEntity.ok().contentType(XML).body(seoService.sitemapIndex());
    }

    @GetMapping(value = "/sitemap-posts.xml", produces = "application/xml; charset=UTF-8")
    public ResponseEntity<String> postsSitemap() {
        return ResponseEntity.ok().contentType(XML).body(seoService.postsSitemap());
    }

    @GetMapping(value = "/news-sitemap.xml", produces = "application/xml; charset=UTF-8")
    public ResponseEntity<String> newsSitemap() {
        return ResponseEntity.ok().contentType(XML).body(seoService.newsSitemap());
    }

    @GetMapping(value = "/rss.xml", produces = "application/rss+xml; charset=UTF-8")
    public ResponseEntity<String> rss() {
        return ResponseEntity.ok().contentType(RSS).body(seoService.rssFeed());
    }

    @GetMapping(value = "/robots.txt", produces = "text/plain; charset=UTF-8")
    public ResponseEntity<String> robots() {
        return ResponseEntity.ok().contentType(TEXT).body(seoService.robotsTxt());
    }
}
