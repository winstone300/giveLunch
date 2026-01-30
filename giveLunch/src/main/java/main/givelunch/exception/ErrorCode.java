package main.givelunch.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "FOOD_NOT_FOUND", "해당 음식을 찾을 수 없습니다."),
    INVALID_FOOD_NAME(HttpStatus.BAD_REQUEST, "INVALID_FOOD_NAME", "음식 이름은 필수입니다."),
    INVALID_USERNAME(HttpStatus.BAD_REQUEST, "INVALID_USERNAME", "아이디를 입력해주세요."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호를 입력해주세요."),
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "이메일을 입력해주세요."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "비밀번호 확인이 일치하지 않습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "EMAIL_NOT_VERIFIED", "이메일 인증을 완료해주세요."),
    INVALID_EMAIL_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "INVALID_EMAIL_VERIFICATION_CODE", "이메일 인증번호가 올바르지 않습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILED", "이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}