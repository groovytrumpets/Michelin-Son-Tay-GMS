package com.g42.platform.gms.warehouse.api.dto.issue;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
public class CreateStockIssueRequest {

    @NotNull
    private Integer warehouseId;

    @NotNull
    private IssueType issueType;

    @NotBlank
    private String issueReason;

    private Integer serviceTicketId;

    private String receiverName;
    private String receiverPhone;
    private String licensePlate;

    @NotEmpty
    @Valid
    private List<IssueItemRequest> items;

    @Data
    public static class IssueItemRequest {
        @NotNull
        private Integer itemId;

        @NotNull
        @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false)
        private BigDecimal quantity;

        /** Discount rate % (0-100), optional */
        private BigDecimal discountRate = BigDecimal.ZERO;

        private Integer entryItemId;
    }
}
