package com.g42.platform.gms.document.repository;

import com.g42.platform.gms.document.entity.DocumentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Integer> {

    List<DocumentTemplate> findByKindIdOrderByDefaultTemplateDescNameAsc(Integer kindId);

    List<DocumentTemplate> findByKindIdAndActiveTrueOrderByDefaultTemplateDescNameAsc(Integer kindId);

    long countByKindId(Integer kindId);

    /**
     * Bỏ cờ mặc định của mọi mẫu khác cùng dạng. Gọi ngay trước khi đặt mẫu mới
     * làm mặc định, để không bao giờ có hai mẫu cùng mặc định trong một dạng.
     */
    @Modifying
    @Query("UPDATE DocumentTemplate t SET t.defaultTemplate = false "
            + "WHERE t.kindId = :kindId AND t.templateId <> :keepTemplateId")
    void clearDefaultForKind(@Param("kindId") Integer kindId,
                             @Param("keepTemplateId") Integer keepTemplateId);
}
