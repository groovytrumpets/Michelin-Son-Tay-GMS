package com.g42.platform.gms.vehicle.support;

import java.util.Locale;

/**
 * Khoá so trùng biển số: viết hoa, bỏ mọi ký tự không phải chữ và số.
 *
 * Biển số lưu nguyên văn nên "30K-86694", "30k 866.94" và "30K86694" là ba chuỗi khác
 * nhau — nhưng là cùng MỘT xe. Mọi chỗ kiểm tra trùng biển số phải so bằng khoá này
 * (cột vehicle.plate_key, có UNIQUE từ changeset 037), không so chuỗi nguyên văn.
 *
 * Không phụ thuộc Spring để changeset Liquibase 037-3 dùng chung đúng quy tắc này.
 */
public final class PlateKeys {

    private PlateKeys() {
    }

    public static String normalize(String raw) {
        if (raw == null) return null;
        String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return cleaned.isEmpty() ? null : cleaned;
    }
}
