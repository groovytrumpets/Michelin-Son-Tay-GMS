package com.g42.platform.gms.marketing.navmenu.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.navmenu.api.dto.NavMenuItemDto;
import com.g42.platform.gms.marketing.navmenu.app.NavMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Menu điều hướng cho trang khách — không cần đăng nhập. */
@RestController
@RequestMapping("/api/public/nav-menu")
@RequiredArgsConstructor
public class PublicNavMenuController {

    private final NavMenuService navMenuService;

    @GetMapping("/{locationCode}")
    public ResponseEntity<ApiResponse<List<NavMenuItemDto.ItemDto>>> getMenu(@PathVariable String locationCode) {
        return ResponseEntity.ok(ApiResponses.success(navMenuService.getPublicTree(locationCode)));
    }
}
