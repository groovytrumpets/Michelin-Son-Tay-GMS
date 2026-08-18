package com.g42.platform.gms.marketing.itempost.app;

import com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostJpa;
import com.g42.platform.gms.marketing.itempost.infrastructure.repository.ItemPostJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Đưa bài viết phụ tùng đã hẹn giờ lên sóng. Chạy mỗi phút — đủ để giờ đăng
 * chính xác tới phút mà không tạo tải đáng kể vì truy vấn đi thẳng vào index
 * (status, published_at).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemPostPublishScheduler {

    private final ItemPostJpaRepo itemPostRepo;

    @Scheduled(fixedDelay = 60_000L)
    @Transactional
    public void publishDuePosts() {
        LocalDateTime now = LocalDateTime.now();
        List<ItemPostJpa> due = itemPostRepo.findByStatusAndScheduledAtLessThanEqualAndDeletedAtIsNull(
                ItemPostStatus.SCHEDULED, now);
        if (due.isEmpty()) return;

        for (ItemPostJpa post : due) {
            post.setStatus(ItemPostStatus.PUBLISHED);
            // Lấy chính giờ đã hẹn làm mốc đăng để thứ tự bài không phụ thuộc
            // vào thời điểm job tình cờ chạy.
            post.setPublishedAt(post.getScheduledAt() != null ? post.getScheduledAt() : now);
            post.setScheduledAt(null);
            post.setUpdatedAt(now);
        }
        itemPostRepo.saveAll(due);
        log.info("Đã tự động đăng {} bài viết phụ tùng tới hạn", due.size());
    }
}
