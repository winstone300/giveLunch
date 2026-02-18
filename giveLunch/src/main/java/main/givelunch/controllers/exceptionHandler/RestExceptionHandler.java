package main.givelunch.controllers.exceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import main.givelunch.dto.ErrorResponseDto;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.FoodNotFoundException;
import main.givelunch.exception.ValidationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class RestExceptionHandler {
    // 서비스 로직에서 FoodNotFoundException이 명시적으로 throw될 때 호출
    @ExceptionHandler(FoodNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> foodNotFound(FoodNotFoundException e) {
        return buildErrorResponse(e.getErrorCode());
    }

    // @RequestBody + @Valid 검증에서 DTO 필드 제약이 실패할 때 호출
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        DefaultMessageSourceResolvable::getDefaultMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));
        return buildFieldErrorResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    // @RequestParam/@PathVariable 등 메서드 파라미터 제약 검증이 실패할 때 호출.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> fieldErrors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        this::extractPath,
                        ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));
        return buildFieldErrorResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    // 서비스 계층에서 ValidationException이 throw될 때 호출
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(ValidationException e) {
        return buildErrorResponse(e.getErrorCode());
    }

    // 요청 본문 역직렬화(JSON 문법 오류, 타입 불일치 등)가 실패할 때 호출
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return buildErrorResponse(ErrorCode.VALIDATION_ERROR);
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(ErrorCode errorCode) {
        ErrorResponseDto response = new ErrorResponseDto(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    private ResponseEntity<ErrorResponseDto> buildFieldErrorResponse(
            ErrorCode errorCode,
            Map<String, String> fieldErrors) {
        ErrorResponseDto response = new ErrorResponseDto(errorCode.getCode(), errorCode.getMessage(), fieldErrors);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    private String extractPath(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < path.length()) {
            return path.substring(lastDot + 1);
        }
        return path;
    }
}
