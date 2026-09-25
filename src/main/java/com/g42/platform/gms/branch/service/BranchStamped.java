package com.g42.platform.gms.branch.service;

/**
 * Entity mang cột {@code branch_id} được {@link BranchStampListener} tự điền lúc INSERT.
 *
 * <p>Entity nào cài interface này thì nên khai báo cột {@code updatable = false}: domain →
 * JPA (MapStruct) copy cả giá trị null, nên một luồng cập nhật cũ không biết tới branchId sẽ
 * xoá mất xưởng của phiếu. Đổi xưởng chủ động thì đi đường riêng (UPDATE thẳng).
 */
public interface BranchStamped {

    Integer getBranchId();

    void setBranchId(Integer branchId);
}
