package hello.liveclass_be_b.settlement.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record TotalSettlementResponse(
        LocalDate startDate,
        LocalDate endDate,
        Long totalSettlementAmount,
        List<CreatorSettlement> creators
) {

    @Builder
    public record CreatorSettlement(
            String creatorId,
            String creatorName,
            Long totalSalesAmount,
            Long totalRefundAmount,
            Long netSalesAmount,
            Long platformFeeAmount,
            Long settlementAmount,
            Integer salesCount,
            Integer cancelCount
    ) {
    }

    public static TotalSettlementResponse of(
            LocalDate startDate,
            LocalDate endDate,
            List<CreatorSettlement> creators
    ) {
        long totalSettlementAmount = creators.stream()
                .mapToLong(CreatorSettlement::settlementAmount)
                .sum();

        return TotalSettlementResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalSettlementAmount(totalSettlementAmount)
                .creators(creators)
                .build();
    }
}
