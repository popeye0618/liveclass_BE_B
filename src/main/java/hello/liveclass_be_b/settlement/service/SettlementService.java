package hello.liveclass_be_b.settlement.service;

import hello.liveclass_be_b.settlement.dto.MonthlySettlementResponse;
import hello.liveclass_be_b.settlement.dto.TotalSettlementResponse;

import java.time.LocalDate;

public interface SettlementService {

    MonthlySettlementResponse getMonthlySettlementByCreator(String creatorId, String date);

    TotalSettlementResponse getTotalSettlement(LocalDate startDate, LocalDate endDate);
}
