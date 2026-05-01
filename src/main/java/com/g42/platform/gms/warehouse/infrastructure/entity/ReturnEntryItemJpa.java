package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Chi tiết từng sản phẩm trong phiếu hoàn hàng.
 * Mỗi item có condition_note riêng và có thể upload ảnh lỗi riêng
 * qua warehouse_attachment (ref_type = RETURN_ENTRY_ITEM, ref_id = return_item_id).
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

    /** Allocation liên quan đến dòng trả */
    @Column(name = "allocation_id")
    private Integer allocationId;

    /** Dòng issue gốc tương ứng để trả đúng sản phẩm đã xuất */
    @Column(name = "source_issue_item_id")
    private Integer sourceIssueItemId;

    /** Lô nhập (entry_item_id) sẽ nhận hàng trả nếu trả về đúng lô */
    @Column(name = "entry_item_id")
    private Integer entryItemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** Mô tả tình trạng lỗi cụ thể của sản phẩm này */
    @Column(name = "condition_note", columnDefinition = "TEXT", nullable = false)
    private String conditionNote;

    /**
     * true = đây là item đổi mới (xuất ra cho khách), chỉ dùng khi returnType = EXCHANGE.
     * false (default) = item trả về.
     */
    @Column(name = "is_exchange_item", nullable = false)
    private boolean exchangeItem = false;
}
