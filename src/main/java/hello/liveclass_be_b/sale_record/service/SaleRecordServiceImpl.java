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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaleRecordServiceImpl implements SaleRecordService {

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");
    private final SaleRecordRepository saleRecordRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public CoursePurchaseResponse purchaseCourse(CoursePurchaseRequest request) {

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new BusinessException(CourseErrorCode.COURSE_NOT_FOUND));

        SaleRecord saleRecord = SaleRecord.builder()
                .id(request.id())
                .course(course)
                .studentId(request.studentId())
                .amount(request.amount())
                .paidAt(request.paidAt())
                .build();

        saleRecordRepository.save(saleRecord);

        return CoursePurchaseResponse.from(saleRecord);
    }

    @Override
    public List<SaleRecordResponse> findSaleRecordsByCreator(String creatorId, LocalDate startDate, LocalDate endDate) {

        validateDateRange(startDate, endDate);

        List<SaleRecord> saleRecords;

        if (startDate == null && endDate == null) {
            saleRecords = saleRecordRepository.findByCourse_Creator_Id(creatorId);
        } else {
            OffsetDateTime startInclusive = startDate.atStartOfDay().atOffset(KST);
            OffsetDateTime endExclusive = endDate.plusDays(1).atStartOfDay().atOffset(KST);

            saleRecords = saleRecordRepository
                    .findByCreatorIdAndPaidAtRange(
                            creatorId,
                            startInclusive,
                            endExclusive
                    );
        }

        return saleRecords.stream()
                .map(SaleRecordResponse::from)
                .toList();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null && endDate != null) || (startDate != null && endDate == null)) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST_PARAMETER, "시작일과 종료일은 함께 입력해야 합니다.");
        }

        if (startDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST_PARAMETER, "시작일은 종료일보다 늦을 수 없습니다.");
        }
    }
}
