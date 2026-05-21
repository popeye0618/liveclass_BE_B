package hello.liveclass_be_b.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode{

    // 400
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "GLOBAL_400_001", "잘못된 입력값입니다."),
    INVALID_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "GLOBAL_400_002", "요청 파라미터가 올바르지 않습니다."),
    METHOD_ARGUMENT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "GLOBAL_400_003", "요청 값의 타입이 올바르지 않습니다."),
    HTTP_MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "GLOBAL_400_004", "요청 본문을 읽을 수 없습니다."),

    // 404
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "GLOBAL_404_001", "요청한 리소스를 찾을 수 없습니다."),

    // 405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "GLOBAL_405_001", "지원하지 않는 HTTP 메서드입니다."),

    // 500
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL_500_001", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
