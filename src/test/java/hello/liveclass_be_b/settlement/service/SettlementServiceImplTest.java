package hello.liveclass_be_b.settlement.service;

import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import hello.liveclass_be_b.cancel_record.repository.CancelRecordRepository;
import hello.liveclass_be_b.course.entity.Course;
import hello.liveclass_be_b.creator.entity.Creator;
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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementServiceImpl 테스트")
class SettlementServiceImplTest {

    @Mock
    private SaleRecordRepository saleRecordRepository;

    @Mock
    private CancelRecordRepository cancelRecordRepository;

    @Mock
    private PlatformFeePolicy platformFeePolicy;

    @Mock
    private CreatorRepository creatorRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementExcelGenerator settlementExcelGenerator;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Test
    @DisplayName("크리에이터의 월별 정산 금액을 조회한다")
    void getMonthlySettlementByCreator() {
        // given
        String creatorId = "creator-1";
        String date = "2025-03";
        OffsetDateTime start = OffsetDateTime.parse("2025-03-01T00:00:00+09:00");
        OffsetDateTime end = OffsetDateTime.parse("2025-04-01T00:00:00+09:00");

        List<SaleRecord> saleRecords = List.of(
                createSaleRecord("sale-1", createCourse("course-1", createCreator("creator-1", "김강사")), "student-1", 50000L, "2025-03-05T10:00:00+09:00"),
                createSaleRecord("sale-2", createCourse("course-1", createCreator("creator-1", "김강사")), "student-2", 50000L, "2025-03-15T14:30:00+09:00"),
                createSaleRecord("sale-3", createCourse("course-2", createCreator("creator-1", "김강사")), "student-3", 80000L, "2025-03-20T09:00:00+09:00"),
                createSaleRecord("sale-4", createCourse("course-2", createCreator("creator-1", "김강사")), "student-4", 80000L, "2025-03-22T11:00:00+09:00")
        );

        List<CancelRecord> cancelRecords = List.of(
                createCancelRecord("cancel-1", saleRecords.get(2), 80000L, "2025-03-25T10:00:00+09:00"),
                createCancelRecord("cancel-2", saleRecords.get(3), 30000L, "2025-03-28T10:00:00+09:00")
        );

        given(saleRecordRepository.findByCreatorIdAndPaidAtRange(creatorId, start, end))
                .willReturn(saleRecords);
        given(cancelRecordRepository.findByCreatorIdAndCanceledAtRange(creatorId, start, end))
                .willReturn(cancelRecords);
        given(platformFeePolicy.calculateFee(150000L))
                .willReturn(30000L);

        // when
        MonthlySettlementResponse response = settlementService.getMonthlySettlementByCreator(creatorId, date);

        // then
        assertThat(response.totalSalesAmount()).isEqualTo(260000L);
        assertThat(response.totalRefundAmount()).isEqualTo(110000L);
        assertThat(response.netSalesAmount()).isEqualTo(150000L);
        assertThat(response.platformFeeAmount()).isEqualTo(30000L);
        assertThat(response.settlementAmount()).isEqualTo(120000L);
        assertThat(response.salesCount()).isEqualTo(4);
        assertThat(response.cancelCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("판매와 취소가 없는 월은 0원으로 조회한다")
    void getMonthlySettlementByCreatorEmptyMonth() {
        // given
        String creatorId = "creator-3";
        String date = "2025-03";
        OffsetDateTime start = OffsetDateTime.parse("2025-03-01T00:00:00+09:00");
        OffsetDateTime end = OffsetDateTime.parse("2025-04-01T00:00:00+09:00");

        given(saleRecordRepository.findByCreatorIdAndPaidAtRange(creatorId, start, end))
                .willReturn(List.of());
        given(cancelRecordRepository.findByCreatorIdAndCanceledAtRange(creatorId, start, end))
                .willReturn(List.of());
        given(platformFeePolicy.calculateFee(0L))
                .willReturn(0L);

        // when
        MonthlySettlementResponse response = settlementService.getMonthlySettlementByCreator(creatorId, date);

        // then
        assertThat(response.totalSalesAmount()).isEqualTo(0L);
        assertThat(response.totalRefundAmount()).isEqualTo(0L);
        assertThat(response.netSalesAmount()).isEqualTo(0L);
        assertThat(response.platformFeeAmount()).isEqualTo(0L);
        assertThat(response.settlementAmount()).isEqualTo(0L);
        assertThat(response.salesCount()).isEqualTo(0);
        assertThat(response.cancelCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("조회 연월 형식이 올바르지 않으면 예외가 발생한다")
    void getMonthlySettlementFailWhenDateFormatInvalid() {
        // when & then
        assertThatThrownBy(() -> settlementService.getMonthlySettlementByCreator("creator-1", "2025/03"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_REQUEST_PARAMETER);

        verifyNoInteractions(saleRecordRepository, cancelRecordRepository, platformFeePolicy);
    }

    @Test
    @DisplayName("운영자 기간별 정산 현황을 조회한다")
    void getTotalSettlement() {
        // given
        LocalDate startDate = LocalDate.of(2025, 3, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        OffsetDateTime start = OffsetDateTime.parse("2025-03-01T00:00:00+09:00");
        OffsetDateTime end = OffsetDateTime.parse("2025-04-01T00:00:00+09:00");

        Creator creator1 = createCreator("creator-1", "김강사");
        Creator creator2 = createCreator("creator-2", "이강사");

        Course course1 = createCourse("course-1", creator1);
        Course course2 = createCourse("course-2", creator1);
        Course course3 = createCourse("course-3", creator2);

        SaleRecord sale1 = createSaleRecord("sale-1", course1, "student-1", 50000L, "2025-03-05T10:00:00+09:00");
        SaleRecord sale2 = createSaleRecord("sale-2", course1, "student-2", 50000L, "2025-03-15T14:30:00+09:00");
        SaleRecord sale3 = createSaleRecord("sale-3", course2, "student-3", 80000L, "2025-03-20T09:00:00+09:00");
        SaleRecord sale4 = createSaleRecord("sale-4", course2, "student-4", 80000L, "2025-03-22T11:00:00+09:00");
        SaleRecord sale6 = createSaleRecord("sale-6", course3, "student-6", 60000L, "2025-03-10T16:00:00+09:00");

        List<SaleRecord> saleRecords = List.of(sale1, sale2, sale3, sale4, sale6);

        List<CancelRecord> cancelRecords = List.of(
                createCancelRecord("cancel-1", sale3, 80000L, "2025-03-25T10:00:00+09:00"),
                createCancelRecord("cancel-2", sale4, 30000L, "2025-03-28T10:00:00+09:00")
        );

        given(saleRecordRepository.findAllByPaidAtRange(start, end))
                .willReturn(saleRecords);
        given(cancelRecordRepository.findAllByCanceledAtRange(start, end))
                .willReturn(cancelRecords);
        given(platformFeePolicy.calculateFee(150000L))
                .willReturn(30000L);
        given(platformFeePolicy.calculateFee(60000L))
                .willReturn(12000L);

        // when
        TotalSettlementResponse response = settlementService.getTotalSettlement(startDate, endDate);

        // then
        assertThat(response.startDate()).isEqualTo(startDate);
        assertThat(response.endDate()).isEqualTo(endDate);
        assertThat(response.totalSettlementAmount()).isEqualTo(168000L);
        assertThat(response.creators()).hasSize(2);

        TotalSettlementResponse.CreatorSettlement creator1Settlement = response.creators().get(0);
        assertThat(creator1Settlement.creatorId()).isEqualTo("creator-1");
        assertThat(creator1Settlement.totalSalesAmount()).isEqualTo(260000L);
        assertThat(creator1Settlement.totalRefundAmount()).isEqualTo(110000L);
        assertThat(creator1Settlement.netSalesAmount()).isEqualTo(150000L);
        assertThat(creator1Settlement.platformFeeAmount()).isEqualTo(30000L);
        assertThat(creator1Settlement.settlementAmount()).isEqualTo(120000L);
        assertThat(creator1Settlement.salesCount()).isEqualTo(4);
        assertThat(creator1Settlement.cancelCount()).isEqualTo(2);

        TotalSettlementResponse.CreatorSettlement creator2Settlement = response.creators().get(1);
        assertThat(creator2Settlement.creatorId()).isEqualTo("creator-2");
        assertThat(creator2Settlement.totalSalesAmount()).isEqualTo(60000L);
        assertThat(creator2Settlement.totalRefundAmount()).isEqualTo(0L);
        assertThat(creator2Settlement.netSalesAmount()).isEqualTo(60000L);
        assertThat(creator2Settlement.platformFeeAmount()).isEqualTo(12000L);
        assertThat(creator2Settlement.settlementAmount()).isEqualTo(48000L);
        assertThat(creator2Settlement.salesCount()).isEqualTo(1);
        assertThat(creator2Settlement.cancelCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("운영자 기간 조회에서 시작일이 종료일보다 늦으면 예외가 발생한다")
    void getTotalSettlementFailWhenStartDateIsAfterEndDate() {
        // given
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        // when & then
        assertThatThrownBy(() -> settlementService.getTotalSettlement(startDate, endDate))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_REQUEST_PARAMETER);

        verifyNoInteractions(saleRecordRepository, cancelRecordRepository, platformFeePolicy);
    }

    @Test
    @DisplayName("운영자가 월별 정산을 생성하면 PENDING 상태로 저장한다")
    void createMonthlySettlement() {
        // given
        SettlementCreateRequest request = new SettlementCreateRequest("creator-1", "2025-03");
        Creator creator = createCreator("creator-1", "김강사");

        OffsetDateTime start = OffsetDateTime.parse("2025-03-01T00:00:00+09:00");
        OffsetDateTime end = OffsetDateTime.parse("2025-04-01T00:00:00+09:00");

        List<SaleRecord> saleRecords = List.of(
                createSaleRecord("sale-1", createCourse("course-1", creator), "student-1", 50000L, "2025-03-05T10:00:00+09:00"),
                createSaleRecord("sale-2", createCourse("course-1", creator), "student-2", 50000L, "2025-03-15T14:30:00+09:00"),
                createSaleRecord("sale-3", createCourse("course-2", creator), "student-3", 80000L, "2025-03-20T09:00:00+09:00"),
                createSaleRecord("sale-4", createCourse("course-2", creator), "student-4", 80000L, "2025-03-22T11:00:00+09:00")
        );

        List<CancelRecord> cancelRecords = List.of(
                createCancelRecord("cancel-1", saleRecords.get(2), 80000L, "2025-03-25T10:00:00+09:00"),
                createCancelRecord("cancel-2", saleRecords.get(3), 30000L, "2025-03-28T10:00:00+09:00")
        );

        given(creatorRepository.findById(request.creatorId()))
                .willReturn(Optional.of(creator));
        given(settlementRepository.existsByCreatorAndMonth(request.creatorId(), request.settlementMonth()))
                .willReturn(false);
        given(saleRecordRepository.findByCreatorIdAndPaidAtRange(request.creatorId(), start, end))
                .willReturn(saleRecords);
        given(cancelRecordRepository.findByCreatorIdAndCanceledAtRange(request.creatorId(), start, end))
                .willReturn(cancelRecords);
        given(platformFeePolicy.calculateFee(150000L))
                .willReturn(30000L);
        given(platformFeePolicy.getFeeRate())
                .willReturn(20);
        given(settlementRepository.saveAndFlush(any(Settlement.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        SettlementResponse response = settlementService.createMonthlySettlement(request);

        // then
        assertThat(response.creatorId()).isEqualTo("creator-1");
        assertThat(response.settlementMonth()).isEqualTo("2025-03");
        assertThat(response.totalSalesAmount()).isEqualTo(260000L);
        assertThat(response.totalRefundAmount()).isEqualTo(110000L);
        assertThat(response.netSalesAmount()).isEqualTo(150000L);
        assertThat(response.feeRate()).isEqualTo(20);
        assertThat(response.platformFeeAmount()).isEqualTo(30000L);
        assertThat(response.settlementAmount()).isEqualTo(120000L);
        assertThat(response.salesCount()).isEqualTo(4);
        assertThat(response.cancelCount()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(response.calculatedAt()).isNotNull();
        assertThat(response.confirmedAt()).isNull();
        assertThat(response.paidAt()).isNull();

        verify(settlementRepository).saveAndFlush(any(Settlement.class));
    }

    @Test
    @DisplayName("이미 같은 월 정산이 있으면 정산을 생성할 수 없다")
    void createMonthlySettlementFailWhenAlreadyExists() {
        // given
        SettlementCreateRequest request = new SettlementCreateRequest("creator-1", "2025-03");
        Creator creator = createCreator("creator-1", "김강사");

        given(creatorRepository.findById(request.creatorId()))
                .willReturn(Optional.of(creator));
        given(settlementRepository.existsByCreatorAndMonth(request.creatorId(), request.settlementMonth()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> settlementService.createMonthlySettlement(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS);

        verify(settlementRepository, never()).saveAndFlush(any(Settlement.class));
        verifyNoInteractions(saleRecordRepository, cancelRecordRepository, platformFeePolicy);
    }

    @Test
    @DisplayName("PENDING 상태의 정산을 CONFIRMED 상태로 확정한다")
    void confirmSettlement() {
        // given
        Settlement settlement = createSettlement(SettlementStatus.PENDING);

        given(settlementRepository.findById(1L))
                .willReturn(Optional.of(settlement));

        // when
        SettlementResponse response = settlementService.confirmSettlement(1L);

        // then
        assertThat(response.status()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(response.confirmedAt()).isNotNull();
        assertThat(response.paidAt()).isNull();
    }

    @Test
    @DisplayName("CONFIRMED 상태가 아니면 지급 완료 처리할 수 없다")
    void paySettlementFailWhenStatusIsNotConfirmed() {
        // given
        Settlement settlement = createSettlement(SettlementStatus.PENDING);

        given(settlementRepository.findById(1L))
                .willReturn(Optional.of(settlement));

        // when & then
        assertThatThrownBy(() -> settlementService.paySettlement(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(SettlementErrorCode.INVALID_STATUS_CHANGE);
    }

    @Test
    @DisplayName("CONFIRMED 상태의 정산을 PAID 상태로 지급 완료 처리한다")
    void paySettlement() {
        // given
        Settlement settlement = createSettlement(SettlementStatus.CONFIRMED);

        given(settlementRepository.findById(1L))
                .willReturn(Optional.of(settlement));

        // when
        SettlementResponse response = settlementService.paySettlement(1L);

        // then
        assertThat(response.status()).isEqualTo(SettlementStatus.PAID);
        assertThat(response.paidAt()).isNotNull();
    }

    @Test
    @DisplayName("정산 내역 엑셀 파일을 다운로드한다")
    void downloadSettlementExcel() {
        // given
        String startMonth = "2025-01";
        String endMonth = "2025-03";
        List<Settlement> settlements = List.of(createSettlement(SettlementStatus.PAID));
        byte[] content = new byte[]{1, 2, 3};

        given(settlementRepository.findAllByMonthRange(startMonth, endMonth))
                .willReturn(settlements);
        given(settlementExcelGenerator.generate(settlements))
                .willReturn(content);

        // when
        SettlementExcelFile excelFile = settlementService.downloadSettlementExcel(startMonth, endMonth);

        // then
        assertThat(excelFile.fileName()).isEqualTo("settlements_2025-01_2025-03.xlsx");
        assertThat(excelFile.content()).isEqualTo(content);

        verify(settlementRepository).findAllByMonthRange(startMonth, endMonth);
        verify(settlementExcelGenerator).generate(settlements);
    }

    @Test
    @DisplayName("정산 내역이 없어도 헤더만 있는 엑셀 파일을 다운로드한다")
    void downloadSettlementExcelWhenEmpty() {
        // given
        String startMonth = "2025-01";
        String endMonth = "2025-03";
        List<Settlement> settlements = List.of();
        byte[] content = new byte[]{1, 2, 3};

        given(settlementRepository.findAllByMonthRange(startMonth, endMonth))
                .willReturn(settlements);
        given(settlementExcelGenerator.generate(settlements))
                .willReturn(content);

        // when
        SettlementExcelFile excelFile = settlementService.downloadSettlementExcel(startMonth, endMonth);

        // then
        assertThat(excelFile.fileName()).isEqualTo("settlements_2025-01_2025-03.xlsx");
        assertThat(excelFile.content()).isEqualTo(content);

        verify(settlementRepository).findAllByMonthRange(startMonth, endMonth);
        verify(settlementExcelGenerator).generate(settlements);
    }

    @Test
    @DisplayName("엑셀 다운로드 월 형식이 올바르지 않으면 예외가 발생한다")
    void downloadSettlementExcelFailWhenMonthFormatInvalid() {
        // when & then
        assertThatThrownBy(() -> settlementService.downloadSettlementExcel("2025/01", "2025-03"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_REQUEST_PARAMETER);

        verifyNoInteractions(settlementRepository, settlementExcelGenerator);
    }

    @Test
    @DisplayName("엑셀 다운로드 시작 월이 종료 월보다 늦으면 예외가 발생한다")
    void downloadSettlementExcelFailWhenStartMonthIsAfterEndMonth() {
        // when & then
        assertThatThrownBy(() -> settlementService.downloadSettlementExcel("2025-04", "2025-03"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_REQUEST_PARAMETER);

        verifyNoInteractions(settlementRepository, settlementExcelGenerator);
    }

    private Creator createCreator(String id, String name) {
        return Creator.builder()
                .id(id)
                .name(name)
                .build();
    }

    private Course createCourse(String id, Creator creator) {
        return Course.builder()
                .id(id)
                .title("강의")
                .creator(creator)
                .build();
    }

    private SaleRecord createSaleRecord(
            String id,
            Course course,
            String studentId,
            Long amount,
            String paidAt
    ) {
        return SaleRecord.builder()
                .id(id)
                .course(course)
                .studentId(studentId)
                .amount(amount)
                .paidAt(OffsetDateTime.parse(paidAt))
                .build();
    }

    private CancelRecord createCancelRecord(
            String id,
            SaleRecord saleRecord,
            Long amount,
            String canceledAt
    ) {
        return CancelRecord.builder()
                .id(id)
                .saleRecord(saleRecord)
                .amount(amount)
                .canceledAt(OffsetDateTime.parse(canceledAt))
                .build();
    }

    private Settlement createSettlement(SettlementStatus status) {
        return Settlement.builder()
                .creator(createCreator("creator-1", "김강사"))
                .settlementMonth("2025-03")
                .totalSalesAmount(260000L)
                .totalRefundAmount(110000L)
                .netSalesAmount(150000L)
                .feeRate(20)
                .platformFeeAmount(30000L)
                .settlementAmount(120000L)
                .salesCount(4)
                .cancelCount(2)
                .status(status)
                .calculatedAt(OffsetDateTime.parse("2025-04-01T00:00:00+09:00"))
                .build();
    }

}
