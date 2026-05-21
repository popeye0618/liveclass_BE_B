package hello.liveclass_be_b.settlement.service;

import hello.liveclass_be_b.settlement.dto.MonthlySettlementResponse;
import hello.liveclass_be_b.settlement.dto.SettlementCreateRequest;
import hello.liveclass_be_b.settlement.dto.SettlementExcelFile;
import hello.liveclass_be_b.settlement.dto.SettlementResponse;
import hello.liveclass_be_b.settlement.dto.TotalSettlementResponse;

import java.time.LocalDate;

public interface SettlementService {

    MonthlySettlementResponse getMonthlySettlementByCreator(String creatorId, String date);

    TotalSettlementResponse getTotalSettlement(LocalDate startDate, LocalDate endDate);

    SettlementResponse createMonthlySettlement(SettlementCreateRequest request);

    SettlementResponse confirmSettlement(Long settlementId);

    SettlementResponse paySettlement(Long settlementId);

    SettlementExcelFile downloadSettlementExcel(String startMonth, String endMonth);
}
