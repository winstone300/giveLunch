package main.givelunch.controllers.exceptionHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import main.givelunch.dto.ErrorResponseDto;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    @DisplayName("ValidationException 처리: ErrorCode 기준 메시지를 응답")
    void handleValidationExceptionReturnsStatusAndMessage() {
        ValidationException exception = new ValidationException(
                ErrorCode.INVALID_PASSWORD_RESET_CODE,
                "커스텀 메시지"
        );

        ResponseEntity<ErrorResponseDto> response = handler.handleValidationException(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("INVALID_PASSWORD_RESET_CODE");
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_CODE.getMessage());
    }

    @Test
    @DisplayName("ConstraintViolationException 처리: 경로 마지막 필드명을 추출해 반환")
    void handleConstraintViolationExtractsFieldName() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("verifyResetCode.req.email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("올바른 이메일 형식을 입력해주세요.");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponseDto> response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().fields()).containsEntry("email", "올바른 이메일 형식을 입력해주세요.");
    }
}
