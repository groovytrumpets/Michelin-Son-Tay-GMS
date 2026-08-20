package com.g42.platform.gms.marketing.service_catalog.api.controller;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceCreateRequest;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceDetailRespond;
import com.g42.platform.gms.marketing.service_catalog.api.dto.ServiceSumaryRespond;
import com.g42.platform.gms.marketing.service_catalog.application.service.ServiceCatalogService;
import com.g42.platform.gms.warehouse.api.dto.BrandHintDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogCreateDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.dto.HomeStockLocationDto;
import com.g42.platform.gms.warehouse.api.dto.PublicPartDetailDto;
import com.g42.platform.gms.warehouse.app.service.catalog.CatalogItemService;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/home")
public class ServiceController {
    @Autowired
    private final ServiceCatalogService serviceCatalogService;
    @Autowired
    private CatalogItemService catalogItemService;
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<ServiceSumaryRespond>>> getLandingServices() {
        List<ServiceSumaryRespond> respondList = serviceCatalogService.getListActiveServices();
        return ResponseEntity.ok(ApiResponses.success(respondList));
    }
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<ServiceSumaryRespond>>>
    getAllProducts(@RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "10") int size,
                   @RequestParam(required = false) CatalogItemType itemType,
                   @RequestParam(required = false)String search,
                   @RequestParam(required = false) String sortBy,
                   @RequestParam(required = false) BigDecimal minPrice,
                   @RequestParam(required = false) BigDecimal maxPrice,
                   @RequestParam(required = false) String categoryCode,
                   @RequestParam(required = false) Integer brandId,
                   @RequestParam(required = false) Integer productLineId,
                   @RequestParam(required = false) String vehicleBrand,
                   @RequestParam(required = false) String vehicleModel){
        Page<ServiceSumaryRespond> apiResponse = serviceCatalogService.getListProducts(page,size,itemType,search,sortBy,minPrice,maxPrice,categoryCode,brandId,productLineId,vehicleBrand,vehicleModel);
        return ResponseEntity.ok(ApiResponses.success(apiResponse));
    }

    /** Danh mục thương hiệu sản phẩm public, ổn định cho bộ lọc landing page. */
    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<BrandHintDto>>> getProductBrands() {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getAllBrands()));
    }

    /** Danh sách kho/cửa hàng còn hàng của một phụ tùng — public cho trang chi tiết sản phẩm. */
    @GetMapping("/products/{catalogItemId}/stock-locations")
    public ResponseEntity<ApiResponse<List<HomeStockLocationDto>>> getProductStockLocations(@PathVariable Integer catalogItemId) {
        return ResponseEntity.ok(ApiResponses.success(serviceCatalogService.getPublicStockLocations(catalogItemId)));
    }
    /**
     * Chi tiết bổ sung (màu, xuất xứ, dòng sản phẩm, thông số) cho trang bán hàng công khai.
     * Tách riêng khỏi /api/warehouse/** vì các API đó yêu cầu đăng nhập nhân viên —
     * khách vãng lai xem /parts/:id sẽ bị 401 nếu gọi thẳng vào đó.
     */
    @GetMapping("/products/{catalogItemId}/detail")
    public ResponseEntity<ApiResponse<PublicPartDetailDto>> getPublicPartDetail(@PathVariable Integer catalogItemId) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.getPublicPartDetail(catalogItemId)));
    }

    /**
     * Tra catalogItemId từ đường dẫn chữ tuỳ chỉnh (catalog_item.slug) — cho phép
     * link dạng chữ ngay cả khi mặt hàng chưa có "Bài viết" (item_post) đầy đủ.
     */
    @GetMapping("/products/slug/{slug}")
    public ResponseEntity<ApiResponse<Integer>> getCatalogItemIdBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponses.success(catalogItemService.findCatalogItemIdBySlug(slug)));
    }

    @GetMapping("/service/{serviceId}")
    public ResponseEntity<ApiResponse<ServiceDetailRespond>> getServiceDetail(@PathVariable Long serviceId) {
        ServiceDetailRespond serviceDetailRespond = serviceCatalogService.getServiceDetailById(serviceId);
        return ResponseEntity.ok(ApiResponses.success(serviceDetailRespond));
    }
    @GetMapping("/catalog")
    public Long [] getCatalogId(@RequestParam Long[] serviceId) {
        return serviceCatalogService.getArrayOfCatalogId(serviceId);
    }

}
