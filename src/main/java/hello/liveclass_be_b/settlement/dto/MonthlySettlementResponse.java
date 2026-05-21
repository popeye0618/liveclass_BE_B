package hello.liveclass_be_b.settlement.dto;

import lombok.Builder;

@Builder
public record MonthlySettlementResponse(
        Long totalSalesAmount,
        Long totalRefundAmount,
        Long netSalesAmount,
        Long platformFeeAmount,
        Long settlementAmount,
        Integer salesCount,
        Integer cancelCount
) {
}
