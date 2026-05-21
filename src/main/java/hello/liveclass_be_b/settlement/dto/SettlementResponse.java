package hello.liveclass_be_b.settlement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import hello.liveclass_be_b.settlement.entity.Settlement;
import hello.liveclass_be_b.settlement.enums.SettlementStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Builder
public record SettlementResponse(
        Long id,
        String creatorId,
        String creatorName,
        String settlementMonth,
        Long totalSalesAmount,
        Long totalRefundAmount,
        Long netSalesAmount,
        Integer feeRate,
        Long platformFeeAmount,
        Long settlementAmount,
        Integer salesCount,
        Integer cancelCount,
        SettlementStatus status,
        OffsetDateTime calculatedAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime paidAt
) {
    private static final ZoneOffset KST = ZoneOffset.of("+09:00");

    public static SettlementResponse from(Settlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .creatorId(settlement.getCreator().getId())
                .creatorName(settlement.getCreator().getName())
                .settlementMonth(settlement.getSettlementMonth())
                .totalSalesAmount(settlement.getTotalSalesAmount())
                .totalRefundAmount(settlement.getTotalRefundAmount())
                .netSalesAmount(settlement.getNetSalesAmount())
                .feeRate(settlement.getFeeRate())
                .platformFeeAmount(settlement.getPlatformFeeAmount())
                .settlementAmount(settlement.getSettlementAmount())
                .salesCount(settlement.getSalesCount())
                .cancelCount(settlement.getCancelCount())
                .status(settlement.getStatus())
                .calculatedAt(toKst(settlement.getCalculatedAt()))
                .confirmedAt(toKst(settlement.getConfirmedAt()))
                .paidAt(toKst(settlement.getPaidAt()))
                .build();
    }

    private static OffsetDateTime toKst(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime.withOffsetSameInstant(KST);
    }
}
