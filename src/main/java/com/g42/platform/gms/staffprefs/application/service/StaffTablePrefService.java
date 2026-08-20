package com.g42.platform.gms.staffprefs.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.g42.platform.gms.staffprefs.infrastructure.entity.StaffTablePrefJpa;
import com.g42.platform.gms.staffprefs.infrastructure.repository.StaffTablePrefRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffTablePrefService {

    @Autowired
    private StaffTablePrefRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    /** Trả về cấu hình đã lưu (đối tượng JSON tuỳ ý) hoặc null nếu chưa có / lỗi parse. */
    @Transactional(readOnly = true)
    public Object get(Integer staffId, String tableKey) {
        if (staffId == null || tableKey == null || tableKey.isBlank()) return null;
        return repository.findByStaffIdAndTableKey(staffId, tableKey)
                .map(entity -> {
                    try {
                        return objectMapper.readValue(entity.getPrefsJson(), Object.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    /** Upsert cấu hình theo (staffId, tableKey). */
    @Transactional
    public void save(Integer staffId, String tableKey, Object prefs) {
        if (staffId == null) {
            throw new IllegalStateException("Không xác định được nhân viên đăng nhập.");
        }
        if (tableKey == null || tableKey.isBlank()) {
            throw new IllegalArgumentException("Thiếu tableKey.");
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(prefs);
        } catch (Exception e) {
            throw new IllegalArgumentException("Dữ liệu cấu hình không hợp lệ.");
        }

        StaffTablePrefJpa entity = repository.findByStaffIdAndTableKey(staffId, tableKey)
                .orElseGet(StaffTablePrefJpa::new);
        entity.setStaffId(staffId);
        entity.setTableKey(tableKey);
        entity.setPrefsJson(json);
        repository.save(entity);
    }
}
