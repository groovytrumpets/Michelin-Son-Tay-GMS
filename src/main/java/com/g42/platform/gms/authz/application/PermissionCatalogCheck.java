package com.g42.platform.gms.authz.application;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.authz.infrastructure.repository.PermissionJpaRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Đối chiếu hằng số trong {@link PermissionCodes} với bảng {@code permission}
 * lúc khởi động.
 *
 * <p>Hai bên lệch nhau là lỗi âm thầm khó thấy nhất của kiểu phân quyền này:
 * thêm hằng số mà quên seed thì @PreAuthorize chặn hết mọi người mà màn cấu hình
 * không có ô nào để tick; seed mà quên thêm hằng số thì admin tick được một
 * quyền chẳng có tác dụng gì. Chỉ ghi cảnh báo chứ không chặn khởi động, vì DB
 * cũ chưa chạy Liquibase vẫn phải lên được để mà chạy migration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionCatalogCheck {

    private final PermissionJpaRepo permissionRepo;

    @EventListener(ApplicationReadyEvent.class)
    public void verify() {
        Set<String> inDb;
        try {
            inDb = new LinkedHashSet<>(permissionRepo.findAll().stream().map(p -> p.getCode()).toList());
        } catch (Exception e) {
            log.warn("Chưa đọc được bảng permission để đối chiếu danh mục quyền: {}", e.getMessage());
            return;
        }

        Set<String> inCode = codeConstants();

        Set<String> missingInDb = new TreeSet<>(inCode);
        missingInDb.removeAll(inDb);
        Set<String> missingInCode = new TreeSet<>(inDb);
        missingInCode.removeAll(inCode);

        if (missingInDb.isEmpty() && missingInCode.isEmpty()) {
            log.info("Danh mục quyền khớp giữa code và DB ({} mã)", inCode.size());
            return;
        }
        if (!missingInDb.isEmpty()) {
            log.warn("Có hằng số trong PermissionCodes nhưng THIẾU trong bảng permission "
                    + "(màn cấu hình sẽ không hiện, không ai được cấp): {}", missingInDb);
        }
        if (!missingInCode.isEmpty()) {
            log.warn("Có mã trong bảng permission nhưng THIẾU hằng số trong PermissionCodes "
                    + "(tick vào cũng không có tác dụng): {}", missingInCode);
        }
    }

    private Set<String> codeConstants() {
        Set<String> codes = new LinkedHashSet<>();
        for (Field field : PermissionCodes.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) continue;
            try {
                codes.add((String) field.get(null));
            } catch (IllegalAccessException ignored) {
                // hằng số public static final, không vào nhánh này
            }
        }
        return codes;
    }
}
