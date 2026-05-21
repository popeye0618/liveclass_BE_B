package hello.liveclass_be_b.cancel_record.dto;

import hello.liveclass_be_b.cancel_record.entity.CancelRecord;
import hello.liveclass_be_b.sale_record.entity.SaleRecord;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Builder
public record CancelRecordResponse(
        String id,
        String saleRecordId,
        String courseId,
        String studentId,
        Long saleAmount,
        Long refundAmount,
        OffsetDateTime canceledAt
) {
    private static final ZoneOffset KST = ZoneOffset.of("+09:00");

    public static CancelRecordResponse from(CancelRecord cancelRecord) {
        SaleRecord saleRecord = cancelRecord.getSaleRecord();

        return CancelRecordResponse.builder()
                .id(cancelRecord.getId())
                .saleRecordId(saleRecord.getId())
                .courseId(saleRecord.getCourse().getId())
                .studentId(saleRecord.getStudentId())
                .saleAmount(saleRecord.getAmount())
                .refundAmount(cancelRecord.getAmount())
                .canceledAt(cancelRecord.getCanceledAt().withOffsetSameInstant(KST))
                .build();
    }
}
