package hello.liveclass_be_b.cancel_record.service;

import hello.liveclass_be_b.cancel_record.dto.CancelRecordCreateRequest;
import hello.liveclass_be_b.cancel_record.dto.CancelRecordResponse;
import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import hello.liveclass_be_b.cancel_record.error.CancelRecordErrorCode;
import hello.liveclass_be_b.cancel_record.repository.CancelRecordRepository;
import hello.liveclass_be_b.course.entity.Course;
import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import hello.liveclass_be_b.sale_record.repository.SaleRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelRecordServiceImpl 테스트")
class CancelRecordServiceImplTest {

    @Mock
    private CancelRecordRepository cancelRecordRepository;

    @Mock
    private SaleRecordRepository saleRecordRepository;

    @InjectMocks
    private CancelRecordServiceImpl cancelRecordService;

    @Test
    @DisplayName("전액 환불을 등록한다")
    void createCancelRecordSuccessWithFullRefund() {
        // given
        CancelRecordCreateRequest request = createCancelRecordCreateRequest("cancel-1", 80000L);
        SaleRecord saleRecord = createSaleRecord(80000L);

        given(saleRecordRepository.findById(request.saleRecordId())).willReturn(Optional.of(saleRecord));
        given(cancelRecordRepository.sumRefundAmountBySaleRecordId(saleRecord.getId())).willReturn(0L);

        // when
        CancelRecordResponse response = cancelRecordService.createCancelRecord(request);

        // then
        assertThat(response.id()).isEqualTo("cancel-1");
        assertThat(response.saleRecordId()).isEqualTo("sale-1");
        assertThat(response.courseId()).isEqualTo("course-1");
        assertThat(response.studentId()).isEqualTo("student-1");
        assertThat(response.saleAmount()).isEqualTo(80000L);
        assertThat(response.refundAmount()).isEqualTo(80000L);
        assertThat(response.canceledAt()).isEqualTo(OffsetDateTime.parse("2025-03-25T10:00:00+09:00"));

        verify(cancelRecordRepository, times(1)).save(any(CancelRecord.class));
    }

    @Test
    @DisplayName("부분 환불을 등록한다")
    void createCancelRecordSuccessWithPartialRefund() {
        // given
        CancelRecordCreateRequest request = createCancelRecordCreateRequest("cancel-2", 30000L);
        SaleRecord saleRecord = createSaleRecord(80000L);

        given(saleRecordRepository.findById(request.saleRecordId())).willReturn(Optional.of(saleRecord));
        given(cancelRecordRepository.sumRefundAmountBySaleRecordId(saleRecord.getId())).willReturn(0L);

        // when
        CancelRecordResponse response = cancelRecordService.createCancelRecord(request);

        // then
        assertThat(response.id()).isEqualTo("cancel-2");
        assertThat(response.saleAmount()).isEqualTo(80000L);
        assertThat(response.refundAmount()).isEqualTo(30000L);

        verify(cancelRecordRepository, times(1)).save(any(CancelRecord.class));
    }

    @Test
    @DisplayName("누적 환불 금액이 결제 금액을 초과하면 예외가 발생한다")
    void createCancelRecordFailWhenRefundAmountExceeded() {
        // given
        CancelRecordCreateRequest request = createCancelRecordCreateRequest("cancel-3", 60000L);
        SaleRecord saleRecord = createSaleRecord(80000L);

        given(saleRecordRepository.findById(request.saleRecordId())).willReturn(Optional.of(saleRecord));
        given(cancelRecordRepository.sumRefundAmountBySaleRecordId(saleRecord.getId())).willReturn(30000L);

        // when & then
        assertThatThrownBy(() -> cancelRecordService.createCancelRecord(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CancelRecordErrorCode.REFUND_AMOUNT_EXCEEDED);

        verify(cancelRecordRepository, never()).save(any(CancelRecord.class));
    }

    @Test
    @DisplayName("존재하지 않는 판매 내역이면 예외가 발생한다")
    void createCancelRecordFailWhenSaleRecordNotFound() {
        // given
        CancelRecordCreateRequest request = createCancelRecordCreateRequest("cancel-1", 80000L);

        given(saleRecordRepository.findById(request.saleRecordId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cancelRecordService.createCancelRecord(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CancelRecordErrorCode.SALE_RECORD_NOT_FOUND);

        verifyNoInteractions(cancelRecordRepository);
    }

    private CancelRecordCreateRequest createCancelRecordCreateRequest(String id, Long refundAmount) {
        return new CancelRecordCreateRequest(
                id,
                "sale-1",
                refundAmount,
                OffsetDateTime.parse("2025-03-25T10:00:00+09:00")
        );
    }

    private SaleRecord createSaleRecord(Long amount) {
        return SaleRecord.builder()
                .id("sale-1")
                .course(createCourse())
                .studentId("student-1")
                .amount(amount)
                .paidAt(OffsetDateTime.parse("2025-03-20T09:00:00+09:00"))
                .build();
    }

    private Course createCourse() {
        return Course.builder()
                .id("course-1")
                .title("Spring Boot 입문")
                .build();
    }
}
