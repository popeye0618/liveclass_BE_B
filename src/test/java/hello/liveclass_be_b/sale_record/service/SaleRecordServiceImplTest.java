package hello.liveclass_be_b.sale_record.service;

import hello.liveclass_be_b.course.entity.Course;
import hello.liveclass_be_b.course.error.CourseErrorCode;
import hello.liveclass_be_b.course.repository.CourseRepository;
import hello.liveclass_be_b.global.error.BusinessException;
import hello.liveclass_be_b.global.error.GlobalErrorCode;
import hello.liveclass_be_b.sale_record.dto.CoursePurchaseRequest;
import hello.liveclass_be_b.sale_record.dto.CoursePurchaseResponse;
import hello.liveclass_be_b.sale_record.dto.SaleRecordResponse;
import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import hello.liveclass_be_b.sale_record.repository.SaleRecordRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaleRecordServiceImpl 테스트")
class SaleRecordServiceImplTest {

    @Mock
    private SaleRecordRepository saleRecordRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private SaleRecordServiceImpl saleRecordService;

    @Test
    @DisplayName("정상 판매")
    void purchaseSuccess() {
        // given
        CoursePurchaseRequest request = createPurchaseRequest();
        Course course = createCourse();

        given(courseRepository.findById(request.courseId())).willReturn(Optional.of(course));
        given(saleRecordRepository.save(any(SaleRecord.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        CoursePurchaseResponse response = saleRecordService.purchaseCourse(request);

        // then
        assertThat(response.id()).isEqualTo("sale-1");
        assertThat(response.courseId()).isEqualTo("course-1");
        assertThat(response.courseTitle()).isEqualTo("Spring Boot 입문");
        assertThat(response.studentId()).isEqualTo("student-1");
        assertThat(response.amount()).isEqualTo(50000L);
        assertThat(response.paidAt()).isEqualTo(OffsetDateTime.parse("2025-03-05T10:00:00+09:00"));

        verify(saleRecordRepository, times(1)).save(any(SaleRecord.class));
    }

    @Test
    @DisplayName("존재하지 않는 강의로 판매 등록 시 예외 발생")
    void purchaseNotExistCourse() {
        // given
        CoursePurchaseRequest request = createPurchaseRequest();

        given(courseRepository.findById(request.courseId())).willReturn(Optional.empty());

        // then
        Assertions.assertThatThrownBy(() -> saleRecordService.purchaseCourse(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CourseErrorCode.COURSE_NOT_FOUND);

        verify(saleRecordRepository, never()).save(any(SaleRecord.class));
    }

    @Test
    @DisplayName("크리에이터 ID로 판매 내역을 조회한다")
    void findSaleRecordsByCreator() {
        // given
        String creatorId = "creator-1";
        SaleRecord saleRecord = createSaleRecord(
                "sale-1",
                "student-1",
                50000L,
                OffsetDateTime.parse("2025-03-05T10:00:00+09:00")
        );

        given(saleRecordRepository.findByCourse_Creator_Id(creatorId))
                .willReturn(List.of(saleRecord));

        // when
        List<SaleRecordResponse> responses = saleRecordService.findSaleRecordsByCreator(
                creatorId,
                null,
                null
        );

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo("sale-1");
        assertThat(responses.get(0).courseId()).isEqualTo("course-1");
        assertThat(responses.get(0).courseTitle()).isEqualTo("Spring Boot 입문");
        assertThat(responses.get(0).studentId()).isEqualTo("student-1");
        assertThat(responses.get(0).amount()).isEqualTo(50000L);
        assertThat(responses.get(0).paidAt()).isEqualTo(OffsetDateTime.parse("2025-03-05T10:00:00+09:00"));

        verify(saleRecordRepository, times(1)).findByCourse_Creator_Id(creatorId);
    }

    @Test
    @DisplayName("크리에이터 ID와 기간으로 판매 내역을 조회한다")
    void findSaleRecordsByCreatorWithPeriod() {
        // given
        String creatorId = "creator-1";
        LocalDate startDate = LocalDate.of(2025, 3, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);
        OffsetDateTime startInclusive = OffsetDateTime.parse("2025-03-01T00:00:00+09:00");
        OffsetDateTime endExclusive = OffsetDateTime.parse("2025-04-01T00:00:00+09:00");
        SaleRecord saleRecord = createSaleRecord(
                "sale-2",
                "student-2",
                50000L,
                OffsetDateTime.parse("2025-03-31T23:59:59+09:00")
        );

        given(saleRecordRepository.findByCreatorIdAndPaidAtRange(
                creatorId,
                startInclusive,
                endExclusive
        )).willReturn(List.of(saleRecord));

        // when
        List<SaleRecordResponse> responses = saleRecordService.findSaleRecordsByCreator(
                creatorId,
                startDate,
                endDate
        );

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo("sale-2");
        assertThat(responses.get(0).paidAt()).isEqualTo(OffsetDateTime.parse("2025-03-31T23:59:59+09:00"));

        verify(saleRecordRepository, times(1)).findByCreatorIdAndPaidAtRange(
                creatorId,
                startInclusive,
                endExclusive
        );
    }

    @Test
    @DisplayName("조회 기간의 시작일만 있으면 예외가 발생한다")
    void findSaleRecordsFailWhenOnlyStartDateExists() {
        // given
        LocalDate startDate = LocalDate.of(2025, 3, 1);

        // when & then
        Assertions.assertThatThrownBy(() -> saleRecordService.findSaleRecordsByCreator(
                        "creator-1",
                        startDate,
                        null
                ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_REQUEST_PARAMETER);

        verifyNoInteractions(saleRecordRepository);
    }

    @Test
    @DisplayName("조회 기간의 종료일만 있으면 예외가 발생한다")
    void findSaleRecordsFailWhenOnlyEndDateExists() {
        // given
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        // when & then
        Assertions.assertThatThrownBy(() -> saleRecordService.findSaleRecordsByCreator(
                        "creator-1",
                        null,
                        endDate
                ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_REQUEST_PARAMETER);

        verifyNoInteractions(saleRecordRepository);
    }

    @Test
    @DisplayName("조회 기간의 시작일이 종료일보다 늦으면 예외가 발생한다")
    void findSaleRecordsFailWhenStartDateIsAfterEndDate() {
        // given
        LocalDate startDate = LocalDate.of(2025, 4, 1);
        LocalDate endDate = LocalDate.of(2025, 3, 31);

        // when & then
        Assertions.assertThatThrownBy(() -> saleRecordService.findSaleRecordsByCreator(
                        "creator-1",
                        startDate,
                        endDate
                ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(GlobalErrorCode.INVALID_REQUEST_PARAMETER);

        verifyNoInteractions(saleRecordRepository);
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

    private SaleRecord createSaleRecord(
            String id,
            String studentId,
            Long amount,
            OffsetDateTime paidAt
    ) {
        return SaleRecord.builder()
                .id(id)
                .course(createCourse())
                .studentId(studentId)
                .amount(amount)
                .paidAt(paidAt)
                .build();
    }

    private Course createCourse() {
        return Course.builder()
                .id("course-1")
                .title("Spring Boot 입문")
                .build();
    }
}
