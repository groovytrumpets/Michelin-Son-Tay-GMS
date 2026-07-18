package com.g42.platform.gms.systemlog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "system_log", schema = "michelin_garage")
public class SystemLogJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false)
    private Long logId;

    @Column(name = "actor_staff_id")
    private Integer actorStaffId;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "module", nullable = false, length = 40)
    private String module;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "target_type", length = 60)
    private String targetType;

    @Column(name = "target_id", length = 60)
    private String targetId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
