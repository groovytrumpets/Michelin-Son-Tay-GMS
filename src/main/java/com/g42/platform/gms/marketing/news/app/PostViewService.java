package com.g42.platform.gms.marketing.news.app;

import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostJpa;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostViewLogJpa;
import com.g42.platform.gms.marketing.news.infrastructure.repository.PostJpaRepo;
import com.g42.platform.gms.marketing.news.infrastructure.repository.PostViewLogJpaRepo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Ghi nhận lượt xem bài viết.
 *
 * <p>Cùng một người mở đi mở lại bài trong thời gian ngắn chỉ tính một lượt,
 * nếu không con số hiển thị sẽ vô nghĩa và ai cũng có thể tự thổi phồng bằng
 * cách bấm F5. Danh tính người xem lưu dưới dạng băm để không giữ IP thô.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostViewService {

    /** Trong khoảng này, cùng người xem cùng bài chỉ được tính một lượt. */
    private static final int DEDUPE_MINUTES = 30;

    private final PostJpaRepo postRepo;
    private final PostViewLogJpaRepo viewLogRepo;
    private final PublicPostService publicPostService;

    @Value("${news.view.salt:mst-gms-news}")
    private String hashSalt;

    @Transactional
    public PostDtos.ViewResultDto registerView(String slug,
                                               PostDtos.ViewRequest body,
                                               HttpServletRequest httpRequest) {
        PostJpa post = publicPostService.findVisibleEntity(slug).orElse(null);
        if (post == null) {
            return new PostDtos.ViewResultDto(false, null);
        }

        String visitorHash = buildVisitorHash(httpRequest, body == null ? null : body.sessionKey());
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(DEDUPE_MINUTES);

        if (viewLogRepo.existsByPostIdAndVisitorHashAndViewedAtAfter(post.getPostId(), visitorHash, windowStart)) {
            return new PostDtos.ViewResultDto(false, post.getViewCount());
        }

        viewLogRepo.save(buildLog(post.getPostId(), visitorHash, body));
        postRepo.incrementViewCount(post.getPostId());

        long current = (post.getViewCount() == null ? 0L : post.getViewCount()) + 1;
        return new PostDtos.ViewResultDto(true, current);
    }

    private PostViewLogJpa buildLog(Long postId, String visitorHash, PostDtos.ViewRequest body) {
        PostViewLogJpa log = new PostViewLogJpa();
        log.setPostId(postId);
        log.setVisitorHash(visitorHash);
        log.setViewedAt(LocalDateTime.now());
        if (body != null) {
            log.setSessionKey(trim(body.sessionKey(), 64));
            log.setReferrer(trim(body.referrer(), 500));
            log.setUtmSource(trim(body.utmSource(), 120));
            log.setUtmMedium(trim(body.utmMedium(), 120));
            log.setUtmCampaign(trim(body.utmCampaign(), 120));
            log.setUtmContent(trim(body.utmContent(), 120));
            log.setDevice(trim(body.device(), 20));
        }
        return log;
    }

    private String buildVisitorHash(HttpServletRequest request, String sessionKey) {
        String ip = clientIp(request);
        String userAgent = request == null ? "" : String.valueOf(request.getHeader("User-Agent"));
        String material = hashSalt + '|' + ip + '|' + userAgent + '|' + (sessionKey == null ? "" : sessionKey);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(material.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 luôn có mặt trên JVM chuẩn; nhánh này chỉ để thoả trình biên dịch.
            log.warn("Không tạo được SHA-256, quay về mã băm chuỗi", e);
            return Integer.toHexString(material.hashCode());
        }
    }

    /** Ứng dụng chạy sau nginx nên IP thật nằm ở X-Forwarded-For. */
    private String clientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return String.valueOf(request.getRemoteAddr());
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        String text = value.trim();
        if (text.isEmpty()) return null;
        return text.length() <= max ? text : text.substring(0, max);
    }
}
