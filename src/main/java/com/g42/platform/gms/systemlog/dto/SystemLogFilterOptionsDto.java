package com.g42.platform.gms.systemlog.dto;

import java.util.List;

/** Lựa chọn cho các ô lọc ở trang Nhật ký hệ thống. */
public record SystemLogFilterOptionsDto(List<Actor> actors, List<Operation> operations) {

    /** Người từng có thao tác trong nhật ký (kể cả nhân viên đã nghỉ). */
    public record Actor(Integer staffId, String name, String role, long count) {
    }

    public record Operation(String code, String label) {
    }
}
