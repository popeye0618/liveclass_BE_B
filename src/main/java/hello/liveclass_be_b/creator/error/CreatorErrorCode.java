package hello.liveclass_be_b.creator.error;

import hello.liveclass_be_b.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CreatorErrorCode implements ErrorCode {

    CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "CREATOR_404_001", "크리에이터를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
