package hello.liveclass_be_b.settlement.controller;

import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.global.error.GlobalErrorCode;
import hello.liveclass_be_b.settlement.dto.MonthlySettlementResponse;
import hello.liveclass_be_b.settlement.dto.TotalSettlementResponse;
import hello.liveclass_be_b.settlement.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettlementController.class)
@DisplayName("SettlementController 테스트")
class SettlementControllerTest {

    private static final String MONTHLY_SETTLEMENT_URL = "/api/v1/settlements/creators/{creatorId}/monthly";
    private static final String ADMIN_SETTLEMENT_URL = "/api/v1/admin/settlements";

    @Autowired
    private MockMvc mockMvc;

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
}
