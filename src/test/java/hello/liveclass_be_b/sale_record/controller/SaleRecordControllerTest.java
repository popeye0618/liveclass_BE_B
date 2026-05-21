package hello.liveclass_be_b.sale_record.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import hello.liveclass_be_b.course.error.CourseErrorCode;
import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.global.error.GlobalErrorCode;
import hello.liveclass_be_b.sale_record.dto.CoursePurchaseRequest;
import hello.liveclass_be_b.sale_record.dto.CoursePurchaseResponse;
import hello.liveclass_be_b.sale_record.dto.SaleRecordResponse;
import hello.liveclass_be_b.sale_record.service.SaleRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SaleRecordController.class)
@DisplayName("SaleRecordController 테스트")
class SaleRecordControllerTest {

    private static final String PURCHASE_URL = "/api/v1/sale-record/purchase";
    private static final String CREATOR_SALE_RECORDS_URL = "/api/v1/creators/{creatorId}/sale-records";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaleRecordService saleRecordService;

    @Test
    @DisplayName("강의 구매 요청이 정상 처리된다")
    void purchaseCourseSuccess() throws Exception {
        // given
        CoursePurchaseRequest request = createPurchaseRequest();
        CoursePurchaseResponse response = createPurchaseResponse();

        given(saleRecordService.purchaseCourse(any(CoursePurchaseRequest.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(post(PURCHASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("sale-1"))
                .andExpect(jsonPath("$.data.courseId").value("course-1"))
                .andExpect(jsonPath("$.data.courseTitle").value("Spring Boot 입문"))
                .andExpect(jsonPath("$.data.studentId").value("student-1"))
                .andExpect(jsonPath("$.data.amount").value(50000))
                .andExpect(jsonPath("$.data.paidAt").value("2025-03-05T10:00:00+09:00"));

        verify(saleRecordService).purchaseCourse(any(CoursePurchaseRequest.class));
    }

    @Test
    @DisplayName("결제 금액이 0보다 작으면 400을 반환한다")
    void purchaseCourseFailWhenAmountIsNotPositive() throws Exception {
        // given
        CoursePurchaseRequest request = new CoursePurchaseRequest(
                "sale-1",
                "course-1",
                "student-1",
                -1000L,
                OffsetDateTime.parse("2025-03-05T10:00:00+09:00")
        );

        // when & then
        mockMvc.perform(post(PURCHASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL_400_001"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("amount"));

        verifyNoInteractions(saleRecordService);
    }

    @Test
    @DisplayName("존재하지 않는 강의이면 404를 반환한다")
    void purchaseCourseFailWhenCourseNotFound() throws Exception {
        // given
        CoursePurchaseRequest request = createPurchaseRequest();

        given(saleRecordService.purchaseCourse(any(CoursePurchaseRequest.class)))
                .willThrow(new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));

        // when & then
        mockMvc.perform(post(PURCHASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COURSE_001"));

        verify(saleRecordService).purchaseCourse(any(CoursePurchaseRequest.class));
    }

    @Test
    @DisplayName("크리에이터 ID로 판매 내역을 조회한다")
    void findSaleRecordsByCreator() throws Exception {
        // given
        String creatorId = "creator-1";

        given(saleRecordService.findSaleRecordsByCreator(
                eq(creatorId),
                isNull(),
                isNull()
        )).willReturn(List.of(createSaleRecordResponse()));

        // when & then
        mockMvc.perform(get(CREATOR_SALE_RECORDS_URL, creatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("sale-1"))
                .andExpect(jsonPath("$.data[0].courseId").value("course-1"))
                .andExpect(jsonPath("$.data[0].courseTitle").value("Spring Boot 입문"))
                .andExpect(jsonPath("$.data[0].studentId").value("student-1"))
                .andExpect(jsonPath("$.data[0].amount").value(50000))
                .andExpect(jsonPath("$.data[0].paidAt").value("2025-03-05T10:00:00+09:00"));

        verify(saleRecordService).findSaleRecordsByCreator(
                eq(creatorId),
                isNull(),
                isNull()
        );
    }

    @Test
    @DisplayName("크리에이터 ID와 기간으로 판매 내역을 조회한다")
    void findSaleRecordsByCreatorWithPeriod() throws Exception {
        // given
        String creatorId = "creator-1";
        LocalDate startDate = LocalDate.of(2025, 3, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        given(saleRecordService.findSaleRecordsByCreator(
                eq(creatorId),
                eq(startDate),
                eq(endDate)
        )).willReturn(List.of(createSaleRecordResponse()));

        // when & then
        mockMvc.perform(get(CREATOR_SALE_RECORDS_URL, creatorId)
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("sale-1"));

        verify(saleRecordService).findSaleRecordsByCreator(
                eq(creatorId),
                eq(startDate),
                eq(endDate)
        );
    }

    @Test
    @DisplayName("기간 파라미터가 올바르지 않으면 400을 반환한다")
    void findSaleRecordsFailWhenDateRangeIsInvalid() throws Exception {
        // given
        String creatorId = "creator-1";
        LocalDate startDate = LocalDate.of(2025, 3, 1);

        given(saleRecordService.findSaleRecordsByCreator(
                eq(creatorId),
                eq(startDate),
                isNull()
        )).willThrow(new BusinessException(
                GlobalErrorCode.INVALID_REQUEST_PARAMETER,
                "시작일과 종료일은 함께 입력해야 합니다."
        ));

        // when & then
        mockMvc.perform(get(CREATOR_SALE_RECORDS_URL, creatorId)
                        .param("startDate", "2025-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("GLOBAL_400_002"));

        verify(saleRecordService).findSaleRecordsByCreator(
                eq(creatorId),
                eq(startDate),
                isNull()
        );
    }

    private CoursePurchaseRequest createPurchaseRequest() {
        return new CoursePurchaseRequest(
                "sale-1",
                "course-1",
                "student-1",
                50000L,
                OffsetDateTime.parse("2025-03-05T10:00:00+09:00")
        );
    }

    private CoursePurchaseResponse createPurchaseResponse() {
        return CoursePurchaseResponse.builder()
                .id("sale-1")
                .courseId("course-1")
                .courseTitle("Spring Boot 입문")
                .studentId("student-1")
                .amount(50000L)
                .paidAt(OffsetDateTime.parse("2025-03-05T10:00:00+09:00"))
                .build();
    }

    private SaleRecordResponse createSaleRecordResponse() {
        return SaleRecordResponse.builder()
                .id("sale-1")
                .courseId("course-1")
                .courseTitle("Spring Boot 입문")
                .studentId("student-1")
                .amount(50000L)
                .paidAt(OffsetDateTime.parse("2025-03-05T10:00:00+09:00"))
                .build();
    }
}
