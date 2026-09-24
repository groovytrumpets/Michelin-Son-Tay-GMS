package com.g42.platform.gms.customer.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customer.api.dto.CustomerCreateDto;
import com.g42.platform.gms.customer.api.dto.CustomerDuplicateCheckDto;
import com.g42.platform.gms.customer.api.dto.CustomerPhonesDto;
import com.g42.platform.gms.customer.application.service.CustomerPhoneService;
import com.g42.platform.gms.customer.api.dto.CustomerUpdateDto;
import com.g42.platform.gms.customer.api.dto.TaxLookupDto;
import com.g42.platform.gms.customer.application.service.CustomerService;
import com.g42.platform.gms.customer.application.service.TaxLookupService;
import com.g42.platform.gms.customer.domain.entity.CustomerProfile;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@AllArgsConstructor
@RequestMapping("/api/admin/customer/")
public class CustomerController {
    /**
     * Các API đọc (tìm khách, xem hồ sơ, tra MST, kiểm tra trùng) không đòi
     * CUSTOMER_VIEW vì màn lập lịch, bán phụ tùng, báo giá, tìm kiếm nhanh cũng
     * gọi để chọn khách. Nhưng phải là nhân viên: token của khách cũng qua được
     * isAuthenticated(), thiếu vế sau thì khách đăng nhập đọc được cả danh bạ.
     */
    private static final String STAFF_ONLY = "isAuthenticated() and !hasRole('CUSTOMER')";

    @Autowired
    CustomerService customerService;
    @Autowired
    TaxLookupService taxLookupService;
    @Autowired
    CustomerPhoneService customerPhoneService;

    /** Tra cứu doanh nghiệp theo mã số thuế để tự động điền hồ sơ đối tác. */
    @GetMapping("tax-lookup")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<ApiResponse<TaxLookupDto>> lookupByTaxCode(@RequestParam String taxCode) {
        return ResponseEntity.ok(ApiResponses.success(taxLookupService.lookup(taxCode)));
    }

    // Nhập Excel (/customer-excel-import) cũng tạo khách qua endpoint này, nên ai
    // có quyền nhập file thì vẫn tạo được dù không được tick "Thêm mới".
    @PostMapping("create")
    @PreAuthorize("hasAnyAuthority('" + PermissionCodes.CUSTOMER_CREATE + "','" + PermissionCodes.CUSTOMER_IMPORT + "')")
    @Auditable(action = "CREATE", module = "CUSTOMER", description = "Tạo hồ sơ khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerCreateDto>> createCustomer(@RequestBody CustomerCreateDto customerDto) {
        return ResponseEntity.ok(ApiResponses.success(customerService.createNewCustomer(customerDto)));
    }
    /**
     * Kiểm tra SĐT/email đã có hồ sơ nào giữ chưa. Form thêm khách gọi liên tục khi
     * nhân viên gõ để chặn tạo trùng ngay tại chỗ thay vì báo lỗi lúc bấm lưu.
     */
    @GetMapping("check-duplicate")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<ApiResponse<CustomerDuplicateCheckDto>> checkDuplicate(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer excludeCustomerId) {
        return ResponseEntity.ok(ApiResponses.success(customerService.checkDuplicate(phone, email, excludeCustomerId)));
    }

    @GetMapping("getAllCustomer")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<ApiResponse<Page<CustomerProfile>>> getAllCustomerProfile(@RequestParam(defaultValue = "0") int page,
                                                                                    @RequestParam(defaultValue = "10") int size,
                                                                                    @RequestParam(required = false) LocalDate date,
                                                                                    @RequestParam(required = false) Boolean isGuest,
                                                                                    @RequestParam(required = false) String search,
                                                                                    @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponses.success(customerService.getListOfAllCustomerProfile(page, size, date, isGuest, search, status)));
    }
    @PutMapping("{customerId}/update")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_EDIT + "')")
    @Auditable(action = "UPDATE", module = "CUSTOMER", description = "Cập nhật hồ sơ khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerCreateDto>> updateProfile(@PathVariable Integer customerId,@RequestBody CustomerUpdateDto customerUpdateDto) {
        return ResponseEntity.ok(ApiResponses.success(customerService.updateCustomer(customerId, customerUpdateDto)));
    }
    /** Số chính + các số phụ của khách (một khách nhiều số, changeset 037). */
    @GetMapping("{customerId}/phones")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<ApiResponse<CustomerPhonesDto>> getPhones(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(customerPhoneService.getPhones(customerId)));
    }

    /** Ghi đè toàn bộ danh sách số của khách; số nào đã thuộc khách khác thì chặn. */
    @PutMapping("{customerId}/phones")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_EDIT + "')")
    @Auditable(action = "UPDATE", module = "CUSTOMER", description = "Cập nhật số điện thoại của khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerPhonesDto>> replacePhones(@PathVariable Integer customerId,
                                                                        @RequestBody CustomerPhonesDto request) {
        return ResponseEntity.ok(ApiResponses.success(customerPhoneService.replacePhones(customerId, request)));
    }

    @GetMapping("{customerId}")
    @PreAuthorize(STAFF_ONLY)
    public ResponseEntity<ApiResponse<CustomerProfile>> getCustomerProfile(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(customerService.findByCustomerId(customerId)));
    }
    @PutMapping("{customerId}/delete")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_DELETE + "')")
    @Auditable(action = "DELETE", module = "CUSTOMER", severity = "CRITICAL", description = "Xóa hồ sơ khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerProfile>> deleteProfile(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(customerService.deleteCustomer(customerId)));
    }
    /**
     * Nhân viên kích hoạt hộ tài khoản khách nhập từ sổ cũ. Giữ nguyên PIN 6 số cuối
     * số điện thoại, khách buộc phải đổi ở lần đăng nhập đầu.
     */
    @PatchMapping("{customerId}/activate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_EDIT + "')")
    @Auditable(action = "UPDATE", module = "CUSTOMER", severity = "WARNING",
            description = "Kích hoạt tài khoản khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerProfile>> activateProfile(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(customerService.activateCustomer(customerId)));
    }

    @PutMapping("{customerId}/locked")
    @PreAuthorize("hasAuthority('" + PermissionCodes.CUSTOMER_EDIT + "')")
    @Auditable(action = "UPDATE", module = "CUSTOMER", severity = "WARNING", description = "Khóa tài khoản khách hàng", targetType = "CUSTOMER")
    public ResponseEntity<ApiResponse<CustomerProfile>> lockedProfile(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(customerService.lockedCustomer(customerId)));
    }
}
