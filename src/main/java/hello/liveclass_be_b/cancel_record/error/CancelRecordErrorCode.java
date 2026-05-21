package hello.liveclass_be_b.cancel_record.error;

import hello.liveclass_be_b.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CancelRecordErrorCode implements ErrorCode {

    SALE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "CANCEL_404_001", "판매 내역을 찾을 수 없습니다."),
    REFUND_AMOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "CANCEL_400_001", "누적 환불 금액은 결제 금액을 초과할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}