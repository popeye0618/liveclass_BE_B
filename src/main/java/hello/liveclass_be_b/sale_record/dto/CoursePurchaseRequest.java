package hello.liveclass_be_b.sale_record.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record CoursePurchaseRequest(
        @NotBlank(message = "판매 내역 ID는 필수입니다.")
        String id,

        @NotBlank(message = "강의 ID는 필수입니다.")
        String courseId,

        @NotBlank(message = "수강생 ID는 필수입니다.")
        String studentId,

        @NotNull(message = "결제 금액은 필수입니다.")
        @Positive(message = "결제 금액은 0보다 커야 합니다.")
        Long amount,

        @NotNull(message = "결제 일시는 필수입니다.")
        @PastOrPresent(message = "결제 일시는 미래일 수 없습니다.")
        OffsetDateTime paidAt
) {
}
