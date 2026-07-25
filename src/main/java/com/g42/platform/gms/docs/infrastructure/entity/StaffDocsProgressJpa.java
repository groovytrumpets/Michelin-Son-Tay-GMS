package com.g42.platform.gms.docs.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "staff_docs_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"staff_id", "topic_id"})
})
public class StaffDocsProgressJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "topic_id", nullable = false, length = 50)
    private String topicId;

    @Column(name = "section_id", nullable = false, length = 50)
    private String sectionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED

    @Column(name = "score")
    private Integer score = 0;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        if (status == null) {
            status = "NOT_STARTED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
    }
}
