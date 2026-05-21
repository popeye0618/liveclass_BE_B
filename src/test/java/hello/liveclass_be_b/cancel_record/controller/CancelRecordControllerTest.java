package hello.liveclass_be_b.cancel_record.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hello.liveclass_be_b.cancel_record.dto.CancelRecordCreateRequest;
import hello.liveclass_be_b.cancel_record.dto.CancelRecordResponse;
import hello.liveclass_be_b.cancel_record.error.CancelRecordErrorCode;
import hello.liveclass_be_b.cancel_record.service.CancelRecordService;
import hello.liveclass_be_b.global.error.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CancelRecordController.class)
@DisplayName("CancelRecordController 테스트")
class CancelRecordControllerTest {

    private static final String CANCEL_RECORD_URL = "/api/v1/cancel-records";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CancelRecordService cancelRecordService;

    @Test
    @DisplayName("취소 내역을 등록한다")
    void createCancelRecordSuccess() throws Exception {
        // given
        CancelRecordCreateRequest request = createCancelRecordCreateRequest(80000L);
        CancelRecordResponse response = createCancelRecordResponse(80000L);

        given(cancelRecordService.createCancelRecord(any(CancelRecordCreateRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post(CANCEL_RECORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("cancel-1"))
                .andExpect(jsonPath("$.data.saleRecordId").value("sale-1"))
                .andExpect(jsonPath("$.data.courseId").value("course-1"))
                .andExpect(jsonPath("$.data.studentId").value("student-1"))
                .andExpect(jsonPath("$.data.saleAmount").value(80000))
                .andExpect(jsonPath("$.data.refundAmount").value(80000))
                .andExpect(jsonPath("$.data.canceledAt").value("2025-03-25T10:00:00+09:00"));

        verify(cancelRecordService).createCancelRecord(any(CancelRecordCreateRequest.class));
    }

    @Test
    @DisplayName("환불 금액이 0보다 작으면 400을 반환한다")
    void createCancelRecordFailWhenAmountIsNotPositive() throws Exception {
        // given
        CancelRecordCreateRequest request = createCancelRecordCreateRequest(-1000L);

        // when & then
        mockMvc.perform(post(CANCEL_RECORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL_400_001"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("refundAmount"));

        verifyNoInteractions(cancelRecordService);
    }

    @Test
    @DisplayName("존재하지 않는 판매 내역이면 404를 반환한다")
    void createCancelRecordFailWhenSaleRecordNotFound() throws Exception {
        // given
        CancelRecordCreateRequest request = createCancelRecordCreateRequest(80000L);

        given(cancelRecordService.createCancelRecord(any(CancelRecordCreateRequest.class)))
                .willThrow(new BusinessException(CancelRecordErrorCode.SALE_RECORD_NOT_FOUND));

        // when & then
        mockMvc.perform(post(CANCEL_RECORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CANCEL_404_001"));

        verify(cancelRecordService).createCancelRecord(any(CancelRecordCreateRequest.class));
    }

    @Test
    @DisplayName("누적 환불 금액이 결제 금액을 초과하면 400을 반환한다")
    void createCancelRecordFailWhenRefundAmountExceeded() throws Exception {
        // given
        CancelRecordCreateRequest request = createCancelRecordCreateRequest(90000L);

        given(cancelRecordService.createCancelRecord(any(CancelRecordCreateRequest.class)))
                .willThrow(new BusinessException(CancelRecordErrorCode.REFUND_AMOUNT_EXCEEDED));

        // when & then
        mockMvc.perform(post(CANCEL_RECORD_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CANCEL_400_001"));

        verify(cancelRecordService).createCancelRecord(any(CancelRecordCreateRequest.class));
    }

    private CancelRecordCreateRequest createCancelRecordCreateRequest(Long refundAmount) {
        return new CancelRecordCreateRequest(
                "cancel-1",
                "sale-1",
                refundAmount,
                OffsetDateTime.parse("2025-03-25T10:00:00+09:00")
        );
    }

    private CancelRecordResponse createCancelRecordResponse(Long refundAmount) {
        return CancelRecordResponse.builder()
                .id("cancel-1")
                .saleRecordId("sale-1")
                .courseId("course-1")
                .studentId("student-1")
                .saleAmount(80000L)
                .refundAmount(refundAmount)
                .canceledAt(OffsetDateTime.parse("2025-03-25T10:00:00+09:00"))
                .build();
    }
}
