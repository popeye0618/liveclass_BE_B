package hello.liveclass_be_b.settlement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "settlement")
public class SettlementProperties {
    private int platformFeeRate = 20;
}
