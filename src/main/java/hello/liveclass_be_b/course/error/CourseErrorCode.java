package hello.liveclass_be_b.course.error;

import hello.liveclass_be_b.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE_001", "강의를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
