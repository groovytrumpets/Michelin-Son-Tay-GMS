package com.g42.platform.gms.marketing.navmenu.infrastructure.entity;

import com.g42.platform.gms.marketing.navmenu.domain.NavDropdownLayout;
import com.g42.platform.gms.marketing.navmenu.domain.NavItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một mục trên thanh điều hướng.
 *
 * <p>Bảng tự tham chiếu: {@code parent} rỗng là mục cấp 1 nằm trên thanh menu,
 * có giá trị là mục nằm trong dropdown của mục cha. Mục cha và mục con dùng
 * chung một bộ thuộc tính nên không tách thành hai bảng.
 */
@Getter
@Setter
@Entity
@Table(name = "nav_menu_item")
public class NavMenuItemJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nav_item_id", nullable = false)
    private Integer navItemId;

    @Column(name = "location_code", nullable = false, length = 50)
    private String locationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private NavMenuItemJpa parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<NavMenuItemJpa> children = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private NavItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "dropdown_layout", length = 20)
    private NavDropdownLayout dropdownLayout;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "badge_text", length = 30)
    private String badgeText;

    @Column(name = "target_path", length = 500)
    private String targetPath;

    @Column(name = "open_in_new_tab")
    private Boolean openInNewTab;

    /** Cột chứa mục này khi dropdown dùng kiểu COLUMNS. */
    @Column(name = "column_index")
    private Integer columnIndex;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
