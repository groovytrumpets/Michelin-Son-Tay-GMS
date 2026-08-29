package com.g42.platform.gms.customerimport.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customerimport.api.dto.CustomerVisitStatsDto;
import com.g42.platform.gms.customerimport.api.dto.LegacyVisitDetailDto;
import com.g42.platform.gms.customerimport.api.dto.LegacyVisitUpdateDto;
import com.g42.platform.gms.customerimport.application.service.CustomerImportService;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import com.g42.platform.gms.customerimport.application.service.CustomerVisitStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Lần cuối khách đến xưởng và số lần đến, gộp phiếu dịch vụ với lịch sử nhập từ sổ cũ.
 * Khớp contract FE: src/services/customerVisitService.js.
 *
 * Lễ tân cũng cần đọc được để biết nên gọi nhắc ai, nên không giới hạn ở quản lý như
 * phần nhập dữ liệu.
 */
@RestController
@RequestMapping("/api/admin/customer-visits")
public class CustomerVisitStatsController {

    @Autowired
    private CustomerVisitStatsService service;

    /** Ghi vào lượt cũ nằm ở service nhập liệu, để sửa tay và nhập file dùng chung quy tắc. */
    @Autowired
    private CustomerImportService importService;

    /**
     * Thống kê cho nhiều khách cùng lúc, dùng cho cột "Lần cuối đến xưởng" ở màn danh
     * bạ. Nhận danh sách mã khách của đúng trang đang hiển thị.
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<CustomerVisitStatsDto>>> stats(
            @RequestParam List<Integer> customerIds) {
        return ResponseEntity.ok(ApiResponses.success(
                new ArrayList<>(service.statsOf(customerIds).values())));
    }

    @GetMapping("/{customerId}/stats")
    public ResponseEntity<ApiResponse<CustomerVisitStatsDto>> statsOfCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(service.statsOf(customerId)));
    }

    /**
     * Sửa tay một lượt cũ ngay trên hồ sơ khách. Không đổi được xe của lượt — muốn đổi
     * thì sửa lại cả lô ở màn nhập sổ cũ, vì còn kéo theo tạo xe và chuyển chủ sở hữu.
     */
    @PutMapping("/legacy/{legacyVisitId}")
    @Auditable(action = "UPDATE", module = "CUSTOMER",
            description = "Sửa lượt dịch vụ nhập từ sổ cũ", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<Void>> updateLegacyVisit(@PathVariable Integer legacyVisitId,
                                                               @RequestBody LegacyVisitUpdateDto dto) {
        importService.updateLegacyVisit(legacyVisitId, dto);
        return ResponseEntity.ok(ApiResponses.success(null));
    }

    /** Xoá một lượt cũ ghi nhầm. Khách và xe giữ nguyên. */
    @DeleteMapping("/legacy/{legacyVisitId}")
    @Auditable(action = "DELETE", module = "CUSTOMER", severity = "WARNING",
            description = "Xoá lượt dịch vụ nhập từ sổ cũ", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<Void>> deleteLegacyVisit(@PathVariable Integer legacyVisitId) {
        importService.deleteLegacyVisit(legacyVisitId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }

    /** Lịch sử lượt nhập từ sổ cũ của một khách, cho khối "Lịch sử trước khi dùng phần mềm". */
    @GetMapping("/{customerId}/legacy")
    public ResponseEntity<ApiResponse<List<LegacyVisitDetailDto>>> legacyHistory(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(service.legacyHistoryOf(customerId)));
    }
}
