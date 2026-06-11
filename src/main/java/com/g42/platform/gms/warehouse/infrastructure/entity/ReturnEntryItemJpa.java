package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.warehouse.domain.enums.DefectCause;
import com.g42.platform.gms.warehouse.domain.enums.ReturnReason;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Chi tiết từng sản phẩm trong phiếu hoàn hàng.
 *
 * - condition_note   : mô tả tình trạng sản phẩm khi trả
 * - return_reason    : WRONG_TYPE (nhầm kiểu/mẫu) hoặc DEFECTIVE (hàng lỗi)
 * - defect_cause     : nguyên nhân lỗi (TECHNICIAN / WAREHOUSE / SUPPLIER), chỉ khi DEFECTIVE
 * - responsible_staff_id : nhân viên chịu trách nhiệm, bắt buộc khi cause là TECHNICIAN/WAREHOUSE
 * - defective_warehouse_id : kho hàng lỗi đích, được tự động điền khi confirm
 */
@Entity
@Table(name = "return_entry_item")
@Data
public class ReturnEntryItemJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_item_id")
    private Integer returnItemId;

    @Column(name = "return_id", nullable = false)
    private Integer returnId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "allocation_id")
    private Integer allocationId;

    @Column(name = "source_issue_item_id")
    private Integer sourceIssueItemId;

    @Column(name = "entry_item_id")
    private Integer entryItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "condition_note", columnDefinition = "TEXT", nullable = false)
    private String conditionNote;

    @Column(name = "is_exchange_item", nullable = false)
    private boolean exchangeItem = false;

    /**
     * Phân loại lý do hoàn: WRONG_TYPE hoặc DEFECTIVE.
     * Nullable để tương thích ngược với dữ liệu cũ (coi như WRONG_TYPE nếu null).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "return_reason", length = 20, nullable = false)
    private ReturnReason returnReason = ReturnReason.WRONG_TYPE;

    /**
     * Nguyên nhân gây lỗi – chỉ có giá trị khi return_reason = DEFECTIVE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "defect_cause", length = 20)
    private DefectCause defectCause;

    /**
     * Nhân viên chịu trách nhiệm về lỗi.
     * Bắt buộc khi defect_cause = TECHNICIAN hoặc WAREHOUSE.
     */
    @Column(name = "responsible_staff_id")
    private Integer responsibleStaffId;

    /**
     * Kho hàng lỗi nhận hàng, được resolve tự động khi confirm.
     * NULL khi return_reason = WRONG_TYPE.
     */
    @Column(name = "defective_warehouse_id")
    private Integer defectiveWarehouseId;
}
