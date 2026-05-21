package hello.liveclass_be_b.settlement.entity;

import hello.liveclass_be_b.creator.entity.Creator;
import hello.liveclass_be_b.settlement.enums.SettlementStatus;
import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.settlement.error.SettlementErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_settlement_creator_month",
                        columnNames = {"creator_id", "settlement_month"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Column(nullable = false, length = 7)
    private String settlementMonth;

    @Column(nullable = false)
    private Long totalSalesAmount;

    @Column(nullable = false)
    private Long totalRefundAmount;

    @Column(nullable = false)
    private Long netSalesAmount;

    @Column(nullable = false)
    private Integer feeRate;

    @Column(nullable = false)
    private Long platformFeeAmount;

    @Column(nullable = false)
    private Long settlementAmount;

    @Column(nullable = false)
    private Integer salesCount;

    @Column(nullable = false)
    private Integer cancelCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(nullable = false)
    private OffsetDateTime calculatedAt;

    private OffsetDateTime confirmedAt;
    private OffsetDateTime paidAt;

    @Builder
    public Settlement(
            Creator creator,
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
        this.creator = creator;
        this.settlementMonth = settlementMonth;
        this.totalSalesAmount = totalSalesAmount;
        this.totalRefundAmount = totalRefundAmount;
        this.netSalesAmount = netSalesAmount;
        this.feeRate = feeRate;
        this.platformFeeAmount = platformFeeAmount;
        this.settlementAmount = settlementAmount;
        this.salesCount = salesCount;
        this.cancelCount = cancelCount;
        this.status = status;
        this.calculatedAt = calculatedAt;
        this.confirmedAt = confirmedAt;
        this.paidAt = paidAt;
    }

    public void confirm(OffsetDateTime confirmedAt) {
        if (status != SettlementStatus.PENDING) {
            throw new BusinessException(SettlementErrorCode.INVALID_STATUS_CHANGE);
        }

        this.status = SettlementStatus.CONFIRMED;
        this.confirmedAt = confirmedAt;
    }

    public void pay(OffsetDateTime paidAt) {
        if (status != SettlementStatus.CONFIRMED) {
            throw new BusinessException(SettlementErrorCode.INVALID_STATUS_CHANGE);
        }

        this.status = SettlementStatus.PAID;
        this.paidAt = paidAt;
    }
}
