package com.g42.platform.gms.customer.api.dto;

import com.g42.platform.gms.customer.domain.enums.CustomerRank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRankingDto {
    private Integer customerId;
    private Integer totalPoints;       // Điểm năm hiện tại
    private Integer lifetimePoints;    // Tổng điểm toàn thời gian
    private CustomerRank currentRank;
    private String rankLabelVi;        // Nhãn tiếng Việt: Đồng, Bạc, Vàng, Bạch Kim
    private com.g42.platform.gms.customer.domain.enums.DealerRank currentDealerRank;
    private String dealerRankLabelVi;  // Nhãn Đại lý: Cấp 1, Cấp 2...
    private Integer pointsToNextRank;  // Điểm cần thêm để lên hạng (null nếu đã PLATINUM)
    private String nextRank;           // Hạng tiếp theo
    private LocalDateTime lastActivityAt;
    private Integer pointsResetYear;
}
