package com.g42.platform.gms.marketing.news.app;

import com.g42.platform.gms.marketing.news.domain.PostStatus;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostJpa;
import com.g42.platform.gms.marketing.news.infrastructure.repository.PostJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Đưa bài đã hẹn giờ lên sóng. Chạy mỗi phút — đủ để giờ đăng chính xác tới
 * phút mà không tạo tải đáng kể vì truy vấn đi thẳng vào index (status, published_at).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostPublishScheduler {

    private final PostJpaRepo postRepo;

    @Scheduled(fixedDelay = 60_000L)
    @Transactional
    public void publishDuePosts() {
        LocalDateTime now = LocalDateTime.now();
        List<PostJpa> due = postRepo.findByStatusAndScheduledAtLessThanEqualAndDeletedAtIsNull(
                PostStatus.SCHEDULED, now);
        if (due.isEmpty()) return;

        for (PostJpa post : due) {
            post.setStatus(PostStatus.PUBLISHED);
            // Lấy chính giờ đã hẹn làm mốc đăng để thứ tự bài không phụ thuộc
            // vào thời điểm job tình cờ chạy.
            post.setPublishedAt(post.getScheduledAt() != null ? post.getScheduledAt() : now);
            post.setScheduledAt(null);
            post.setUpdatedAt(now);
        }
        postRepo.saveAll(due);
        log.info("Đã tự động đăng {} bài viết tới hạn", due.size());
    }
}
