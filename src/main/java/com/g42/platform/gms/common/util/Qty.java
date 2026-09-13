package com.g42.platform.gms.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.function.Function;

/**
 * Phép tính số lượng hàng hoá.
 *
 * Số lượng là BigDecimal (DECIMAL(14,3) dưới DB) vì có hàng bán lẻ theo lít/kg.
 * Không dùng double: trừ dần tồn kho hàng trăm lần sẽ cộng dồn sai số nhị phân.
 */
public final class Qty {

    public static final int SCALE = 3;
    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final BigDecimal ONE = BigDecimal.ONE;

    private Qty() {
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    public static BigDecimal of(int value) {
        return BigDecimal.valueOf(value);
    }

    public static BigDecimal of(Number value) {
        if (value == null) return ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Integer || value instanceof Long || value instanceof Short) {
            return BigDecimal.valueOf(value.longValue());
        }
        return new BigDecimal(value.toString());
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    public static boolean gt(BigDecimal a, BigDecimal b) {
        return nz(a).compareTo(nz(b)) > 0;
    }

    public static boolean lt(BigDecimal a, BigDecimal b) {
        return nz(a).compareTo(nz(b)) < 0;
    }

    public static boolean eq(BigDecimal a, BigDecimal b) {
        return nz(a).compareTo(nz(b)) == 0;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return nz(a).min(nz(b));
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return nz(a).max(nz(b));
    }

    /** max(0, a - b) */
    public static BigDecimal subFloorZero(BigDecimal a, BigDecimal b) {
        return nz(a).subtract(nz(b)).max(ZERO);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return nz(a).add(nz(b));
    }

    public static BigDecimal sub(BigDecimal a, BigDecimal b) {
        return nz(a).subtract(nz(b));
    }

    public static <T> BigDecimal sum(Collection<T> items, Function<T, BigDecimal> getter) {
        BigDecimal total = ZERO;
        if (items == null) return total;
        for (T item : items) {
            total = total.add(nz(getter.apply(item)));
        }
        return total;
    }

    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Số lẻ hoá thành chữ gọn để in thông báo lỗi: 2.500 -> "2.5", 3.000 -> "3". */
    public static String text(BigDecimal value) {
        return nz(value).stripTrailingZeros().toPlainString();
    }
}
