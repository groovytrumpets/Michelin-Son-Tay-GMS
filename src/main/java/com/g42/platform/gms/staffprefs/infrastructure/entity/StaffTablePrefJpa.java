package com.g42.platform.gms.staffprefs.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Cấp 2 (đồng bộ đa thiết bị) của cấu hình bảng (cột ẩn/hiện, thứ tự, độ
 * rộng) theo từng nhân viên — cấp 1 là localStorage ở FE. `prefsJson` là
 * chuỗi JSON đối tượng cấu hình, backend không cần biết cấu trúc bên trong.
 */
@Entity
@Table(name = "staff_table_pref")
@Data
public class StaffTablePrefJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pref_id")
    private Integer prefId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "table_key", nullable = false, length = 100)
    private String tableKey;

    @Lob
    @Column(name = "prefs_json", nullable = false)
    private String prefsJson;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = LocalDateTime.now();
    }
}
