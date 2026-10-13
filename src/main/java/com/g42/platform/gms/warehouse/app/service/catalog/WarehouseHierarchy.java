package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.common.enums.WarehouseTypeEnum;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;

import java.util.*;

/**
 * Cây kho đa cấp dựng từ warehouse.parent_warehouse_id (tự tham chiếu, sâu bao nhiêu cũng được).
 *
 * Tồn kho vẫn nằm ở đúng kho vật lý chứa hàng (bảng inventory không đổi); "tồn của kho cha"
 * chỉ là số cộng dồn lúc đọc = hàng của chính nó + mọi kho con cháu. Kho lỗi (DEFECTIVE) không
 * được cộng lên cha vì hàng lỗi không bán/xuất được — tự xem kho lỗi thì vẫn thấy hàng của nó.
 */
public final class WarehouseHierarchy {

    private final Map<Integer, Warehouse> byId = new LinkedHashMap<>();
    private final Map<Integer, List<Integer>> childrenOf = new HashMap<>();

    public WarehouseHierarchy(Collection<Warehouse> warehouses) {
        for (Warehouse w : warehouses) {
            byId.put(w.getWarehouseId(), w);
        }
        for (Warehouse w : warehouses) {
            Integer parentId = w.getParentWarehouseId();
            if (parentId != null && byId.containsKey(parentId) && !parentId.equals(w.getWarehouseId())) {
                childrenOf.computeIfAbsent(parentId, k -> new ArrayList<>()).add(w.getWarehouseId());
            }
        }
    }

    public List<Integer> childIds(Integer warehouseId) {
        return childrenOf.getOrDefault(warehouseId, List.of());
    }

    /** Mọi kho con cháu của rootId (không gồm chính nó). Có chặn vòng lặp phòng dữ liệu cũ bị lỗi. */
    public Set<Integer> descendantIds(Integer rootId) {
        Set<Integer> result = new LinkedHashSet<>();
        Deque<Integer> stack = new ArrayDeque<>(childIds(rootId));
        while (!stack.isEmpty()) {
            Integer id = stack.pop();
            if (id.equals(rootId) || !result.add(id)) continue;
            stack.addAll(childIds(id));
        }
        return result;
    }

    /**
     * Các kho được cộng vào tồn của rootId: chính nó + con cháu, bỏ nhánh kho lỗi.
     * Nếu rootId bản thân là kho lỗi thì chỉ gồm chính nó.
     */
    public Set<Integer> rollupWarehouseIds(Integer rootId) {
        Set<Integer> result = new LinkedHashSet<>();
        result.add(rootId);
        Deque<Integer> stack = new ArrayDeque<>(childIds(rootId));
        while (!stack.isEmpty()) {
            Integer id = stack.pop();
            Warehouse w = byId.get(id);
            if (w == null || w.getWarehouseType() == WarehouseTypeEnum.DEFECTIVE) continue;
            if (!result.add(id)) continue;
            stack.addAll(childIds(id));
        }
        return result;
    }

    /** true nếu gán candidateParentId làm cha của warehouseId sẽ tạo vòng (chính nó hoặc con cháu của nó). */
    public boolean wouldCreateCycle(Integer warehouseId, Integer candidateParentId) {
        return warehouseId.equals(candidateParentId) || descendantIds(warehouseId).contains(candidateParentId);
    }
}
