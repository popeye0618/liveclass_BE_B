package hello.liveclass_be_b.settlement.service;

import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import hello.liveclass_be_b.cancel_record.repository.CancelRecordRepository;
import hello.liveclass_be_b.creator.entity.Creator;
import hello.liveclass_be_b.creator.error.CreatorErrorCode;
import hello.liveclass_be_b.creator.repository.CreatorRepository;
import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.global.error.GlobalErrorCode;
import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import hello.liveclass_be_b.sale_record.repository.SaleRecordRepository;
import hello.liveclass_be_b.settlement.dto.MonthlySettlementResponse;
import hello.liveclass_be_b.settlement.dto.SettlementCreateRequest;
import hello.liveclass_be_b.settlement.dto.SettlementExcelFile;
import hello.liveclass_be_b.settlement.dto.SettlementResponse;
import hello.liveclass_be_b.settlement.dto.TotalSettlementResponse;
import hello.liveclass_be_b.settlement.entity.Settlement;
import hello.liveclass_be_b.settlement.enums.SettlementStatus;
import hello.liveclass_be_b.settlement.error.SettlementErrorCode;
import hello.liveclass_be_b.settlement.excel.SettlementExcelGenerator;
import hello.liveclass_be_b.settlement.policy.PlatformFeePolicy;
import hello.liveclass_be_b.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final CreatorRepository creatorRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementExcelGenerator settlementExcelGenerator;
    private final PlatformFeePolicy platformFeePolicy;

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");

    @Override
    public MonthlySettlementResponse getMonthlySettlementByCreator(String creatorId, String date) {
        YearMonth yearMonth = parseYearMonth(date);
        SettlementCalculation calculation = calculateMonthlySettlement(creatorId, yearMonth);

        return calculation.toMonthlyResponse();
    }

    @Override
    @Transactional
    public SettlementResponse createMonthlySettlement(SettlementCreateRequest request) {
        YearMonth yearMonth = parseYearMonth(request.settlementMonth());
        String settlementMonth = yearMonth.toString();

        Creator creator = creatorRepository.findById(request.creatorId())
                .orElseThrow(() -> new BusinessException(CreatorErrorCode.CREATOR_NOT_FOUND));

        if (settlementRepository.existsByCreatorAndMonth(creator.getId(), settlementMonth)) {
            throw new BusinessException(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }

        SettlementCalculation calculation = calculateMonthlySettlement(creator.getId(), yearMonth);

        Settlement settlement = Settlement.builder()
                .creator(creator)
                .settlementMonth(settlementMonth)
                .totalSalesAmount(calculation.totalSalesAmount())
                .totalRefundAmount(calculation.totalRefundAmount())
                .netSalesAmount(calculation.netSalesAmount())
                .feeRate(platformFeePolicy.getFeeRate())
                .platformFeeAmount(calculation.platformFeeAmount())
                .settlementAmount(calculation.settlementAmount())
                .salesCount(calculation.salesCount())
                .cancelCount(calculation.cancelCount())
                .status(SettlementStatus.PENDING)
                .calculatedAt(now())
                .build();

        try {
            Settlement savedSettlement = settlementRepository.saveAndFlush(settlement);
            return SettlementResponse.from(savedSettlement);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional
    public SettlementResponse confirmSettlement(Long settlementId) {
        Settlement settlement = getSettlement(settlementId);
        settlement.confirm(now());

        return SettlementResponse.from(settlement);
    }

    @Override
    @Transactional
    public SettlementResponse paySettlement(Long settlementId) {
        Settlement settlement = getSettlement(settlementId);
        settlement.pay(now());

        return SettlementResponse.from(settlement);
    }

    @Override
    public SettlementExcelFile downloadSettlementExcel(String startMonth, String endMonth) {
        YearMonth start = parseSettlementMonth(startMonth);
        YearMonth end = parseSettlementMonth(endMonth);

        if (start.isAfter(end)) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                    "시작 정산 연월은 종료 정산 연월보다 늦을 수 없습니다."
            );
        }

        List<Settlement> settlements = settlementRepository.findAllByMonthRange(
                start.toString(),
                end.toString()
        );

        byte[] content = settlementExcelGenerator.generate(settlements);
        String fileName = "settlements_" + start + "_" + end + ".xlsx";

        return new SettlementExcelFile(fileName, content);
    }

    private Settlement getSettlement(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(SettlementErrorCode.SETTLEMENT_NOT_FOUND));
    }

    private SettlementCalculation calculateMonthlySettlement(String creatorId, YearMonth yearMonth) {
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

        return new SettlementCalculation(
                totalSalesAmount,
                totalRefundAmount,
                netSalesAmount,
                platformFeeAmount,
                settlementAmount,
                salesCount,
                cancelCount
        );
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
        if (date == null) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                    "조회 연월은 yyyy-MM 형식이어야 합니다."
            );
        }

        try {
            return YearMonth.parse(date);
        } catch (DateTimeParseException e) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                    "조회 연월은 yyyy-MM 형식이어야 합니다."
            );
        }
    }

    private YearMonth parseSettlementMonth(String month) {
        if (month == null) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                    "정산 연월은 yyyy-MM 형식이어야 합니다."
            );
        }

        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException(
                    GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                    "정산 연월은 yyyy-MM 형식이어야 합니다."
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

    private OffsetDateTime now() {
        return OffsetDateTime.now(KST);
    }

    private record SettlementCalculation(
            Long totalSalesAmount,
            Long totalRefundAmount,
            Long netSalesAmount,
            Long platformFeeAmount,
            Long settlementAmount,
            Integer salesCount,
            Integer cancelCount
    ) {

        private MonthlySettlementResponse toMonthlyResponse() {
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
    }
}
