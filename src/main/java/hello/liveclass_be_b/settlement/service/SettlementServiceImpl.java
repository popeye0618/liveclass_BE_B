package hello.liveclass_be_b.settlement.service;

import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import hello.liveclass_be_b.cancel_record.repository.CancelRecordRepository;
import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.global.error.GlobalErrorCode;
import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import hello.liveclass_be_b.sale_record.repository.SaleRecordRepository;
import hello.liveclass_be_b.settlement.dto.MonthlySettlementResponse;
import hello.liveclass_be_b.settlement.dto.TotalSettlementResponse;
import hello.liveclass_be_b.settlement.policy.PlatformFeePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private final SaleRecordRepository saleRecordRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final PlatformFeePolicy platformFeePolicy;

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");

    @Override
    public MonthlySettlementResponse getMonthlySettlementByCreator(String creatorId, String date) {
        YearMonth yearMonth = parseYearMonth(date);

        List<SaleRecord> salesRecords = getSalesRecord(creatorId, yearMonth);
        List<CancelRecord> cancelRecords = getCancelRecord(creatorId, yearMonth);

        long totalSalesAmount = salesRecords.stream()
                .mapToLong(SaleRecord::getAmount)
                .sum();

        int salesCount = salesRecords.size();

        long totalRefundAmount = cancelRecords.stream()
                .mapToLong(CancelRecord::getAmount)
                .sum();

        int cancelCount = cancelRecords.size();

        long netSalesAmount = totalSalesAmount - totalRefundAmount;
        long platformFeeAmount = platformFeePolicy.calculateFee(netSalesAmount);
        long settlementAmount = netSalesAmount - platformFeeAmount;

        return MonthlySettlementResponse.builder()
                .totalSalesAmount(totalSalesAmount)
                .totalRefundAmount(totalRefundAmount)
                .netSalesAmount(netSalesAmount)
                .platformFeeAmount(platformFeeAmount)
                .settlementAmount(settlementAmount)
                .salesCount(salesCount)
                .cancelCount(cancelCount)
                .build();
    }

    @Override
    public TotalSettlementResponse getTotalSettlement(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        OffsetDateTime start = startDate.atStartOfDay().atOffset(KST);
        OffsetDateTime end = endDate.plusDays(1).atStartOfDay().atOffset(KST);

        List<SaleRecord> saleRecords = saleRecordRepository.findAllByPaidAtRange(start, end);
        List<CancelRecord> cancelRecords = cancelRecordRepository.findAllByCanceledAtRange(start, end);

        Map<String, CreatorSettlementAccumulator> accumulatorMap = new LinkedHashMap<>();

        for (SaleRecord saleRecord : saleRecords) {
            String creatorId = saleRecord.getCourse().getCreator().getId();
            String creatorName = saleRecord.getCourse().getCreator().getName();

            CreatorSettlementAccumulator accumulator = accumulatorMap.computeIfAbsent(
                    creatorId,
                    id -> new CreatorSettlementAccumulator(id, creatorName)
            );

            accumulator.addSale(saleRecord.getAmount());
        }

        for (CancelRecord cancelRecord : cancelRecords) {
            SaleRecord saleRecord = cancelRecord.getSaleRecord();
            String creatorId = saleRecord.getCourse().getCreator().getId();
            String creatorName = saleRecord.getCourse().getCreator().getName();

            CreatorSettlementAccumulator accumulator = accumulatorMap.computeIfAbsent(
                    creatorId,
                    id -> new CreatorSettlementAccumulator(id, creatorName)
            );

            accumulator.addRefund(cancelRecord.getAmount());
        }

        List<TotalSettlementResponse.CreatorSettlement> creators = accumulatorMap.values()
                .stream()
                .map(CreatorSettlementAccumulator::toResponse)
                .toList();

        return TotalSettlementResponse.of(startDate, endDate, creators);
    }

    @RequiredArgsConstructor
    private class CreatorSettlementAccumulator {

        private final String creatorId;
        private final String creatorName;
        private long totalSalesAmount;
        private long totalRefundAmount;
        private int salesCount;
        private int cancelCount;

        private void addSale(long amount) {
            this.totalSalesAmount += amount;
            this.salesCount++;
        }

        private void addRefund(long amount) {
            this.totalRefundAmount += amount;
            this.cancelCount++;
        }

        private TotalSettlementResponse.CreatorSettlement toResponse() {
            long netSalesAmount = totalSalesAmount - totalRefundAmount;
            long platformFeeAmount = platformFeePolicy.calculateFee(netSalesAmount);
            long settlementAmount = netSalesAmount - platformFeeAmount;

            return TotalSettlementResponse.CreatorSettlement.builder()
                    .creatorId(creatorId)
                    .creatorName(creatorName)
                    .totalSalesAmount(totalSalesAmount)
                    .totalRefundAmount(totalRefundAmount)
                    .netSalesAmount(netSalesAmount)
                    .platformFeeAmount(platformFeeAmount)
                    .settlementAmount(settlementAmount)
                    .salesCount(salesCount)
                    .cancelCount(cancelCount)
                    .build();
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                    "시작일과 종료일은 필수입니다."
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                    "시작일은 종료일보다 늦을 수 없습니다."
            );
        }
    }

    private YearMonth parseYearMonth(String date) {
        try {
            return YearMonth.parse(date);
        } catch (DateTimeParseException e) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                    "조회 연월은 yyyy-MM 형식이어야 합니다."
            );
        }
    }

    private List<SaleRecord> getSalesRecord(String creatorId, YearMonth yearMonth) {
        OffsetDateTime start = yearMonth
                .atDay(1)
                .atStartOfDay()
                .atOffset(KST);

        OffsetDateTime end = yearMonth
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay()
                .atOffset(KST);

        return saleRecordRepository.findByCreatorIdAndPaidAtRange(creatorId, start, end);
    }

    private List<CancelRecord> getCancelRecord(String creatorId, YearMonth yearMonth) {
        OffsetDateTime start = yearMonth
                .atDay(1)
                .atStartOfDay()
                .atOffset(KST);

        OffsetDateTime end = yearMonth
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay()
                .atOffset(KST);

        return cancelRecordRepository.findByCreatorIdAndCanceledAtRange(creatorId, start, end);
    }
}
