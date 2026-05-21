package hello.liveclass_be_b.cancel_record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record CancelRecordCreateRequest(
        @NotBlank(message = "취소 내역 ID는 필수입니다.")
        String id,

        @NotBlank(message = "판매 내역 ID는 필수입니다.")
        String saleRecordId,

        @NotNull(message = "환불 금액은 필수입니다.")
        @Positive(message = "환불 금액은 0보다 커야 합니다.")
        Long refundAmount,

        @NotNull(message = "취소 일시는 필수입니다.")
        @PastOrPresent(message = "취소 일시는 미래일 수 없습니다.")
        OffsetDateTime canceledAt
) {
}
