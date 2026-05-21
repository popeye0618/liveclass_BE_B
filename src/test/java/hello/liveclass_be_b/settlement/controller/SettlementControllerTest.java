package hello.liveclass_be_b.settlement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.global.error.GlobalErrorCode;
import hello.liveclass_be_b.settlement.dto.MonthlySettlementResponse;
import hello.liveclass_be_b.settlement.dto.SettlementCreateRequest;
import hello.liveclass_be_b.settlement.dto.SettlementExcelFile;
import hello.liveclass_be_b.settlement.dto.SettlementResponse;
import hello.liveclass_be_b.settlement.dto.TotalSettlementResponse;
import hello.liveclass_be_b.settlement.enums.SettlementStatus;
import hello.liveclass_be_b.settlement.error.SettlementErrorCode;
import hello.liveclass_be_b.settlement.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettlementController.class)
@DisplayName("SettlementController 테스트")
class SettlementControllerTest {

    private static final String MONTHLY_SETTLEMENT_URL = "/api/v1/settlements/creators/{creatorId}/monthly";
    private static final String ADMIN_SETTLEMENT_URL = "/api/v1/admin/settlements";
    private static final String ADMIN_MONTHLY_SETTLEMENT_URL = "/api/v1/admin/settlements/monthly";
    private static final String ADMIN_SETTLEMENT_CONFIRM_URL = "/api/v1/admin/settlements/{settlementId}/confirm";
    private static final String ADMIN_SETTLEMENT_PAY_URL = "/api/v1/admin/settlements/{settlementId}/pay";
    private static final String ADMIN_SETTLEMENT_EXCEL_URL = "/api/v1/admin/settlements/excel";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SettlementService settlementService;

    @Test
    @DisplayName("크리에이터 월별 정산을 조회한다")
    void getMonthlySettlementByCreator() throws Exception {
        // given
        String creatorId = "creator-1";
        String date = "2025-03";
        MonthlySettlementResponse response = createMonthlySettlementResponse();

        given(settlementService.getMonthlySettlementByCreator(creatorId, date))
                .willReturn(response);

        // when & then
        mockMvc.perform(get(MONTHLY_SETTLEMENT_URL, creatorId)
                        .param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSalesAmount").value(260000))
                .andExpect(jsonPath("$.data.totalRefundAmount").value(110000))
                .andExpect(jsonPath("$.data.netSalesAmount").value(150000))
                .andExpect(jsonPath("$.data.platformFeeAmount").value(30000))
                .andExpect(jsonPath("$.data.settlementAmount").value(120000))
                .andExpect(jsonPath("$.data.salesCount").value(4))
                .andExpect(jsonPath("$.data.cancelCount").value(2));

        verify(settlementService).getMonthlySettlementByCreator(creatorId, date);
    }

    @Test
    @DisplayName("조회 연월 형식이 올바르지 않으면 400을 반환한다")
    void getMonthlySettlementFailWhenDateFormatInvalid() throws Exception {
        // given
        String creatorId = "creator-1";
        String date = "2025/03";

        given(settlementService.getMonthlySettlementByCreator(creatorId, date))
                .willThrow(new BusinessException(
                        GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                        "조회 연월은 yyyy-MM 형식이어야 합니다."
                ));

        // when & then
        mockMvc.perform(get(MONTHLY_SETTLEMENT_URL, creatorId)
                        .param("date", date))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL_400_002"));

        verify(settlementService).getMonthlySettlementByCreator(creatorId, date);
    }

    @Test
    @DisplayName("운영자 기간별 정산 현황을 조회한다")
    void getTotalSettlement() throws Exception {
        // given
        LocalDate startDate = LocalDate.of(2025, 3, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        TotalSettlementResponse response = createTotalSettlementResponse(startDate, endDate);

        given(settlementService.getTotalSettlement(startDate, endDate))
                .willReturn(response);

        // when & then
        mockMvc.perform(get(ADMIN_SETTLEMENT_URL)
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startDate").value("2025-03-01"))
                .andExpect(jsonPath("$.data.endDate").value("2025-03-31"))
                .andExpect(jsonPath("$.data.totalSettlementAmount").value(168000))
                .andExpect(jsonPath("$.data.creators[0].creatorId").value("creator-1"))
                .andExpect(jsonPath("$.data.creators[0].creatorName").value("김강사"))
                .andExpect(jsonPath("$.data.creators[0].totalSalesAmount").value(260000))
                .andExpect(jsonPath("$.data.creators[0].totalRefundAmount").value(110000))
                .andExpect(jsonPath("$.data.creators[0].netSalesAmount").value(150000))
                .andExpect(jsonPath("$.data.creators[0].platformFeeAmount").value(30000))
                .andExpect(jsonPath("$.data.creators[0].settlementAmount").value(120000))
                .andExpect(jsonPath("$.data.creators[0].salesCount").value(4))
                .andExpect(jsonPath("$.data.creators[0].cancelCount").value(2))
                .andExpect(jsonPath("$.data.creators[1].creatorId").value("creator-2"))
                .andExpect(jsonPath("$.data.creators[1].settlementAmount").value(48000));

        verify(settlementService).getTotalSettlement(startDate, endDate);
    }

    @Test
    @DisplayName("운영자 기간 조회에서 시작일이 종료일보다 늦으면 400을 반환한다")
    void getTotalSettlementFailWhenStartDateIsAfterEndDate() throws Exception {
        // given
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        given(settlementService.getTotalSettlement(startDate, endDate))
                .willThrow(new BusinessException(
                        GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                        "시작일은 종료일보다 늦을 수 없습니다."
                ));

        // when & then
        mockMvc.perform(get(ADMIN_SETTLEMENT_URL)
                        .param("startDate", "2025-04-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL_400_002"));

        verify(settlementService).getTotalSettlement(startDate, endDate);
    }

    @Test
    @DisplayName("운영자가 월별 정산을 생성한다")
    void createMonthlySettlement() throws Exception {
        // given
        SettlementCreateRequest request = new SettlementCreateRequest("creator-1", "2025-03");
        SettlementResponse response = createSettlementResponse(SettlementStatus.PENDING);

        given(settlementService.createMonthlySettlement(request))
                .willReturn(response);

        // when & then
        mockMvc.perform(post(ADMIN_MONTHLY_SETTLEMENT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.creatorId").value("creator-1"))
                .andExpect(jsonPath("$.data.settlementMonth").value("2025-03"))
                .andExpect(jsonPath("$.data.totalSalesAmount").value(260000))
                .andExpect(jsonPath("$.data.totalRefundAmount").value(110000))
                .andExpect(jsonPath("$.data.netSalesAmount").value(150000))
                .andExpect(jsonPath("$.data.feeRate").value(20))
                .andExpect(jsonPath("$.data.platformFeeAmount").value(30000))
                .andExpect(jsonPath("$.data.settlementAmount").value(120000))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(settlementService).createMonthlySettlement(request);
    }

    @Test
    @DisplayName("이미 같은 월 정산이 있으면 409를 반환한다")
    void createMonthlySettlementFailWhenAlreadyExists() throws Exception {
        // given
        SettlementCreateRequest request = new SettlementCreateRequest("creator-1", "2025-03");

        given(settlementService.createMonthlySettlement(request))
                .willThrow(new BusinessException(SettlementErrorCode.SETTLEMENT_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(post(ADMIN_MONTHLY_SETTLEMENT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_409_001"));

        verify(settlementService).createMonthlySettlement(request);
    }

    @Test
    @DisplayName("운영자가 정산을 확정한다")
    void confirmSettlement() throws Exception {
        // given
        Long settlementId = 1L;
        SettlementResponse response = createSettlementResponse(SettlementStatus.CONFIRMED);

        given(settlementService.confirmSettlement(settlementId))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch(ADMIN_SETTLEMENT_CONFIRM_URL, settlementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        verify(settlementService).confirmSettlement(settlementId);
    }

    @Test
    @DisplayName("운영자가 정산을 지급 완료 처리한다")
    void paySettlement() throws Exception {
        // given
        Long settlementId = 1L;
        SettlementResponse response = createSettlementResponse(SettlementStatus.PAID);

        given(settlementService.paySettlement(settlementId))
                .willReturn(response);

        // when & then
        mockMvc.perform(patch(ADMIN_SETTLEMENT_PAY_URL, settlementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        verify(settlementService).paySettlement(settlementId);
    }

    @Test
    @DisplayName("잘못된 상태 변경이면 400을 반환한다")
    void changeSettlementStatusFailWhenInvalidStatus() throws Exception {
        // given
        Long settlementId = 1L;

        given(settlementService.paySettlement(settlementId))
                .willThrow(new BusinessException(SettlementErrorCode.INVALID_STATUS_CHANGE));

        // when & then
        mockMvc.perform(patch(ADMIN_SETTLEMENT_PAY_URL, settlementId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SETTLEMENT_400_001"));

        verify(settlementService).paySettlement(settlementId);
    }

    @Test
    @DisplayName("운영자가 정산 내역 엑셀 파일을 다운로드한다")
    void downloadSettlementExcel() throws Exception {
        // given
        String startMonth = "2025-01";
        String endMonth = "2025-03";
        SettlementExcelFile excelFile = new SettlementExcelFile(
                "settlements_2025-01_2025-03.xlsx",
                new byte[]{1, 2, 3}
        );

        given(settlementService.downloadSettlementExcel(startMonth, endMonth))
                .willReturn(excelFile);

        // when & then
        mockMvc.perform(get(ADMIN_SETTLEMENT_EXCEL_URL)
                        .param("startMonth", startMonth)
                        .param("endMonth", endMonth))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("settlements_2025-01_2025-03.xlsx")))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));

        verify(settlementService).downloadSettlementExcel(startMonth, endMonth);
    }

    @Test
    @DisplayName("엑셀 다운로드 월 형식이 올바르지 않으면 400을 반환한다")
    void downloadSettlementExcelFailWhenMonthFormatInvalid() throws Exception {
        // given
        String startMonth = "2025/01";
        String endMonth = "2025-03";

        given(settlementService.downloadSettlementExcel(startMonth, endMonth))
                .willThrow(new BusinessException(
                        GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                        "정산 연월은 yyyy-MM 형식이어야 합니다."
                ));

        // when & then
        mockMvc.perform(get(ADMIN_SETTLEMENT_EXCEL_URL)
                        .param("startMonth", startMonth)
                        .param("endMonth", endMonth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL_400_002"));

        verify(settlementService).downloadSettlementExcel(startMonth, endMonth);
    }

    private MonthlySettlementResponse createMonthlySettlementResponse() {
        return MonthlySettlementResponse.builder()
                .totalSalesAmount(260000L)
                .totalRefundAmount(110000L)
                .netSalesAmount(150000L)
                .platformFeeAmount(30000L)
                .settlementAmount(120000L)
                .salesCount(4)
                .cancelCount(2)
                .build();
    }

    private TotalSettlementResponse createTotalSettlementResponse(LocalDate startDate, LocalDate endDate) {
        List<TotalSettlementResponse.CreatorSettlement> creators = List.of(
                TotalSettlementResponse.CreatorSettlement.builder()
                        .creatorId("creator-1")
                        .creatorName("김강사")
                        .totalSalesAmount(260000L)
                        .totalRefundAmount(110000L)
                        .netSalesAmount(150000L)
                        .platformFeeAmount(30000L)
                        .settlementAmount(120000L)
                        .salesCount(4)
                        .cancelCount(2)
                        .build(),
                TotalSettlementResponse.CreatorSettlement.builder()
                        .creatorId("creator-2")
                        .creatorName("이강사")
                        .totalSalesAmount(60000L)
                        .totalRefundAmount(0L)
                        .netSalesAmount(60000L)
                        .platformFeeAmount(12000L)
                        .settlementAmount(48000L)
                        .salesCount(1)
                        .cancelCount(0)
                        .build()
        );

        return TotalSettlementResponse.of(startDate, endDate, creators);
    }

    private SettlementResponse createSettlementResponse(SettlementStatus status) {
        return SettlementResponse.builder()
                .id(1L)
                .creatorId("creator-1")
                .creatorName("김강사")
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
                .confirmedAt(status == SettlementStatus.PENDING ? null : OffsetDateTime.parse("2025-04-02T00:00:00+09:00"))
                .paidAt(status == SettlementStatus.PAID ? OffsetDateTime.parse("2025-04-03T00:00:00+09:00") : null)
                .build();
    }
}
