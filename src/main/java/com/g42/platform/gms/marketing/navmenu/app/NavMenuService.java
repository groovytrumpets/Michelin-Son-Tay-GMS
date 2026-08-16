package com.g42.platform.gms.marketing.navmenu.app;

import com.g42.platform.gms.marketing.navmenu.api.dto.NavMenuItemDto;
import com.g42.platform.gms.marketing.navmenu.domain.NavDropdownLayout;
import com.g42.platform.gms.marketing.navmenu.domain.NavItemType;
import com.g42.platform.gms.marketing.navmenu.infrastructure.entity.NavMenuItemJpa;
import com.g42.platform.gms.marketing.navmenu.infrastructure.repository.NavMenuItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nghiệp vụ menu điều hướng: dựng cây, sửa mục, sắp xếp lại bằng kéo thả. */
@Service
@RequiredArgsConstructor
public class NavMenuService {

    /**
     * Chốt an toàn chống lồng nhau vô tận — KHÔNG phải giới hạn thiết kế.
     *
     * <p>Giao diện khách vẽ menu con bằng đệ quy nên sâu bao nhiêu cấp cũng hiện
     * được; con số này chỉ để một thao tác kéo thả sai không tạo ra chuỗi lồng
     * dài vô lý mà khách không tài nào rê chuột tới cuối. Phải khớp MAX_DEPTH ở
     * NavMenuConfig.jsx, nếu không người dùng bị chặn mà không hiểu vì sao.
     */
    private static final int MAX_DEPTH = 10;

    private final NavMenuItemJpaRepo navRepo;

    // ---------------------------------------------------------------- đọc

    /** Cây menu cho trang khách — bỏ hết mục đang tắt. */
    @Transactional(readOnly = true)
    public List<NavMenuItemDto.ItemDto> getPublicTree(String locationCode) {
        return buildTree(navRepo.findAllForLocation(locationCode), true);
    }

    /** Cây menu cho màn quản trị — giữ cả mục đang tắt để còn bật lại được. */
    @Transactional(readOnly = true)
    public List<NavMenuItemDto.ItemDto> getAdminTree(String locationCode) {
        return buildTree(navRepo.findAllForLocation(locationCode), false);
    }

    /**
     * Dựng cây từ danh sách phẳng đã lấy trong một truy vấn.
     *
     * <p>Duyệt hai lượt: lượt đầu lập chỉ mục theo khoá, lượt sau nối con vào
     * cha. Cách này không phụ thuộc thứ tự cha/con trong danh sách và không sinh
     * thêm truy vấn nào.
     */
    private List<NavMenuItemDto.ItemDto> buildTree(List<NavMenuItemJpa> flat, boolean activeOnly) {
        Map<Integer, List<NavMenuItemJpa>> childrenByParent = new LinkedHashMap<>();
        List<NavMenuItemJpa> roots = new ArrayList<>();

        for (NavMenuItemJpa item : flat) {
            if (activeOnly && Boolean.FALSE.equals(item.getIsActive())) continue;
            Integer parentId = item.getParent() == null ? null : item.getParent().getNavItemId();
            if (parentId == null) roots.add(item);
            else childrenByParent.computeIfAbsent(parentId, key -> new ArrayList<>()).add(item);
        }

        roots.sort(Comparator.comparing(
                NavMenuItemJpa::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())));

        List<NavMenuItemDto.ItemDto> result = new ArrayList<>();
        for (NavMenuItemJpa root : roots) {
            result.add(toDto(root, childrenByParent));
        }
        return result;
    }

    /** Bản rút gọn cho các API ghi — chỉ trả về chính mục vừa lưu, không kèm cây con. */
    private NavMenuItemDto.ItemDto toDto(NavMenuItemJpa item) {
        return toDto(item, Map.of());
    }

    /** Dựng DTO đệ quy theo bản đồ con-theo-cha đã lập sẵn, không sinh thêm truy vấn. */
    private NavMenuItemDto.ItemDto toDto(NavMenuItemJpa item, Map<Integer, List<NavMenuItemJpa>> childrenByParent) {
        List<NavMenuItemDto.ItemDto> childDtos = childrenByParent
                .getOrDefault(item.getNavItemId(), List.of())
                .stream()
                .sorted(Comparator
                        .comparing(NavMenuItemJpa::getColumnIndex, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(NavMenuItemJpa::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(child -> toDto(child, childrenByParent))
                .toList();

        // Giá trị mặc định của cột áp cho cả những dòng không có dropdown; trả về
        // rỗng cho đúng bản chất thay vì để frontend phải tự đoán khi nào bỏ qua.
        NavDropdownLayout layout = item.getItemType() != null && item.getItemType().hasDropdown()
                ? (item.getDropdownLayout() == null ? NavDropdownLayout.LIST : item.getDropdownLayout())
                : null;

        return new NavMenuItemDto.ItemDto(
                item.getNavItemId(),
                item.getParent() == null ? null : item.getParent().getNavItemId(),
                item.getItemType(),
                layout,
                item.getLabel(),
                item.getDescription(),
                item.getImageUrl(),
                item.getBadgeText(),
                item.getTargetPath(),
                item.getOpenInNewTab(),
                item.getColumnIndex(),
                item.getDisplayOrder(),
                item.getIsActive(),
                childDtos
        );
    }

    // ---------------------------------------------------------------- ghi

    @Transactional
    public NavMenuItemDto.ItemDto create(String locationCode, NavMenuItemDto.SaveRequest request) {
        NavMenuItemJpa item = new NavMenuItemJpa();
        item.setLocationCode(locationCode);
        item.setCreatedAt(LocalDateTime.now());
        item.setParent(resolveParent(request.parentId(), null));
        // Mục mới xuống cuối danh sách của đúng nhánh chứa nó.
        item.setDisplayOrder(navRepo.findMaxDisplayOrder(locationCode, request.parentId()) + 1);

        applyEditableFields(item, request);
        return toDto(navRepo.save(item));
    }

    @Transactional
    public NavMenuItemDto.ItemDto update(Integer navItemId, NavMenuItemDto.SaveRequest request) {
        NavMenuItemJpa item = loadOrThrow(navItemId);
        NavMenuItemJpa newParent = resolveParent(request.parentId(), navItemId);
        // Không chỉ chặn "cha là chính nó" mà cả "cha là cháu chắt của nó" — menu
        // nhiều cấp thì vòng lặp gián tiếp mới là cái dễ lọt.
        guardAgainstCycle(item, newParent);
        item.setParent(newParent);
        applyEditableFields(item, request);
        return toDto(navRepo.save(item));
    }

    /**
     * Áp kết quả kéo thả. Nhận cả cây trong một lần gọi thay vì từng mục một, để
     * thứ tự không rơi vào trạng thái nửa vời nếu có lỗi giữa chừng.
     */
    @Transactional
    public List<NavMenuItemDto.ItemDto> reorder(String locationCode, NavMenuItemDto.ReorderRequest request) {
        List<NavMenuItemDto.ReorderEntry> entries = request.items();
        if (entries == null || entries.isEmpty()) return getAdminTree(locationCode);

        List<NavMenuItemJpa> all = navRepo.findAllForLocation(locationCode);
        Map<Integer, NavMenuItemJpa> byId = new LinkedHashMap<>();
        all.forEach(item -> byId.put(item.getNavItemId(), item));

        // Ảnh chụp quan hệ cha-con TRƯỚC khi sửa, dùng để đo chiều cao nhánh đang kéo.
        Map<Integer, List<NavMenuItemJpa>> childrenByParent = new LinkedHashMap<>();
        all.forEach(item -> {
            if (item.getParent() != null) {
                childrenByParent.computeIfAbsent(item.getParent().getNavItemId(), k -> new ArrayList<>()).add(item);
            }
        });

        for (NavMenuItemDto.ReorderEntry entry : entries) {
            NavMenuItemJpa item = byId.get(entry.navItemId());
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mục menu không tồn tại: " + entry.navItemId());
            }

            NavMenuItemJpa newParent = entry.parentId() == null ? null : byId.get(entry.parentId());
            if (entry.parentId() != null && newParent == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Mục cha không tồn tại: " + entry.parentId());
            }
            guardAgainstCycle(item, newParent);
            guardSubtreeDepth(item, newParent, childrenByParent);

            item.setParent(newParent);
            item.setColumnIndex(entry.columnIndex() == null ? 0 : entry.columnIndex());
            item.setDisplayOrder(entry.displayOrder());
            item.setUpdatedAt(LocalDateTime.now());
        }

        navRepo.saveAll(byId.values());
        return getAdminTree(locationCode);
    }

    /** Xoá mục; khoá ngoại đặt ON DELETE CASCADE nên mục con đi theo mục cha. */
    @Transactional
    public void delete(Integer navItemId) {
        NavMenuItemJpa item = loadOrThrow(navItemId);
        navRepo.delete(item);
    }

    // ------------------------------------------------------------ nội bộ

    private void applyEditableFields(NavMenuItemJpa item, NavMenuItemDto.SaveRequest request) {
        item.setItemType(request.itemType());
        item.setLabel(request.label().trim());
        item.setDescription(blankToNull(request.description()));
        item.setImageUrl(blankToNull(request.imageUrl()));
        item.setBadgeText(blankToNull(request.badgeText()));
        item.setTargetPath(blankToNull(request.targetPath()));
        item.setOpenInNewTab(Boolean.TRUE.equals(request.openInNewTab()));
        item.setColumnIndex(request.columnIndex() == null ? 0 : request.columnIndex());
        item.setIsActive(request.isActive() == null || request.isActive());
        item.setUpdatedAt(LocalDateTime.now());

        // Kiểu trình bày chỉ có ý nghĩa với mục mở dropdown; mục thường để trống
        // cho khỏi hiểu nhầm là có menu con.
        item.setDropdownLayout(request.itemType().hasDropdown()
                ? (request.dropdownLayout() == null ? NavDropdownLayout.LIST : request.dropdownLayout())
                : null);
    }

    private NavMenuItemJpa resolveParent(Integer parentId, Integer selfId) {
        if (parentId == null) return null;
        if (parentId.equals(selfId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Một mục không thể là cha của chính nó");
        }
        NavMenuItemJpa parent = loadOrThrow(parentId);
        guardDepth(parent);
        return parent;
    }

    /**
     * Chặn tạo vòng lặp cha-con. Không có chốt này, kéo một mục cha vào trong
     * chính con của nó sẽ tạo ra nhánh mồ côi mà truy vấn dựng cây không bao giờ
     * đọc tới, và menu sẽ mất mục đó mà không rõ lý do.
     */
    private void guardAgainstCycle(NavMenuItemJpa item, NavMenuItemJpa candidateParent) {
        NavMenuItemJpa cursor = candidateParent;
        int guard = 0;
        while (cursor != null && guard++ < 20) {
            if (cursor.getNavItemId().equals(item.getNavItemId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể đặt mục \"" + item.getLabel() + "\" vào bên trong chính nó");
            }
            cursor = cursor.getParent();
        }
    }

    /** Cấp của một mục: mục ngoài cùng là 1. */
    private int depthOf(NavMenuItemJpa item) {
        int depth = 1;
        NavMenuItemJpa cursor = item;
        while (cursor.getParent() != null && depth < 20) {
            depth++;
            cursor = cursor.getParent();
        }
        return depth;
    }

    /** Mục mới đặt dưới {@code parent} không được vượt quá chốt an toàn về số cấp. */
    private void guardDepth(NavMenuItemJpa parent) {
        if (parent == null) return;
        if (depthOf(parent) >= MAX_DEPTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Menu lồng tối đa " + MAX_DEPTH + " cấp, không thể thêm mục con vào đây nữa");
        }
    }

    /**
     * Kéo cả một nhánh vào chỗ mới thì phải tính cả chiều cao của nhánh đó, không
     * chỉ vị trí điểm thả — nếu không, các mục nằm sâu bên dưới sẽ vượt quá số cấp
     * hiển thị được và biến mất khỏi menu.
     */
    private void guardSubtreeDepth(NavMenuItemJpa moving, NavMenuItemJpa newParent,
                                   Map<Integer, List<NavMenuItemJpa>> childrenByParent) {
        int parentDepth = newParent == null ? 0 : depthOf(newParent);
        int height = subtreeHeight(moving.getNavItemId(), childrenByParent, 0);
        if (parentDepth + height > MAX_DEPTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể đặt \"" + moving.getLabel() + "\" vào đây: menu sẽ lồng quá "
                            + MAX_DEPTH + " cấp");
        }
    }

    private int subtreeHeight(Integer itemId, Map<Integer, List<NavMenuItemJpa>> childrenByParent, int guard) {
        if (guard > 20) return 1;
        List<NavMenuItemJpa> children = childrenByParent.getOrDefault(itemId, List.of());
        int best = 1;
        for (NavMenuItemJpa child : children) {
            best = Math.max(best, 1 + subtreeHeight(child.getNavItemId(), childrenByParent, guard + 1));
        }
        return best;
    }

    private NavMenuItemJpa loadOrThrow(Integer navItemId) {
        return navRepo.findById(navItemId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy mục menu"));
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    /** Các loại mục có thể chọn khi tạo mới — trả cho màn quản trị dựng ô chọn. */
    public List<Map<String, Object>> availableItemTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (NavItemType type : NavItemType.values()) {
            types.add(Map.of(
                    "value", type.name(),
                    "hasDropdown", type.hasDropdown()
            ));
        }
        return types;
    }
}
