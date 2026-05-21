package hello.liveclass_be_b.sale_record.dto;

import hello.liveclass_be_b.course.entity.Course;
import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record CoursePurchaseResponse(
        String id,
        String courseId,
        String courseTitle,
        String studentId,
        Long amount,
        OffsetDateTime paidAt
) {

    public static CoursePurchaseResponse from(SaleRecord saleRecord) {
        Course course = saleRecord.getCourse();

        return CoursePurchaseResponse.builder()
                .id(saleRecord.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .studentId(saleRecord.getStudentId())
                .amount(saleRecord.getAmount())
                .paidAt(saleRecord.getPaidAt())
                .build();
    }
}
