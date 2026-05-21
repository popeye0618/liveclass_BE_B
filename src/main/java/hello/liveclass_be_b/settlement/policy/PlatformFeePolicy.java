package hello.liveclass_be_b.settlement.policy;

public interface PlatformFeePolicy {

    int getFeeRate();

    long calculateFee(long netSalesAmount);
}
