package com.g42.platform.gms.marketing.recruitment.infrastructure.repository;

import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.RecruitmentSettingJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecruitmentSettingJpaRepo extends JpaRepository<RecruitmentSettingJpa, Integer> {

    /**
     * Bảng chỉ có một dòng, nhưng vẫn đọc bằng "dòng đầu tiên theo id" thay vì
     * {@code findById(1)}: nếu ai đó lỡ chèn thêm dòng thứ hai thì ứng dụng vẫn
     * chạy với dòng cũ nhất chứ không trả về rỗng rồi mất sạch cấu hình.
     */
    Optional<RecruitmentSettingJpa> findFirstByOrderBySettingIdAsc();
}
