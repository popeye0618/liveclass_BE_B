package hello.liveclass_be_b.settlement.policy;

import hello.liveclass_be_b.settlement.config.SettlementProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FixedRatePlatformFeePolicy implements PlatformFeePolicy {

    private final SettlementProperties settlementProperties;

    @Override
    public int getFeeRate() {
        return settlementProperties.getPlatformFeeRate();
    }

    @Override
    public long calculateFee(long netSalesAmount) {
        return netSalesAmount * getFeeRate() / 100;
    }
}
