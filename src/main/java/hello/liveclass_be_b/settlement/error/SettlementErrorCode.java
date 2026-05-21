package hello.liveclass_be_b.settlement.error;

import hello.liveclass_be_b.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SettlementErrorCode implements ErrorCode {

    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTLEMENT_404_001", "정산 내역을 찾을 수 없습니다."),
    SETTLEMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "SETTLEMENT_409_001", "이미 해당 월 정산 내역이 존재합니다."),
    INVALID_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "SETTLEMENT_400_001", "현재 상태에서는 요청한 상태 변경을 할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
