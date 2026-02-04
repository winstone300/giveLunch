package main.givelunch.controllers.exceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import main.givelunch.dto.ErrorResponseDto;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.FoodNotFoundException;
import main.givelunch.exception.ValidationException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class RestExceptionHandler {
    // Food data가 없을 때
    @ExceptionHandler(FoodNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> foodNotFound(FoodNotFoundException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponseDto response = new ErrorResponseDto(errorCode.getCode(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(),
                        DefaultMessageSourceResolvable::getDefaultMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));
        return buildValidationErrorResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> fieldErrors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> extractPath(violation),
                        ConstraintViolation::getMessage,
                        (first, second) -> first,
                        LinkedHashMap::new));
        return buildValidationErrorResponse(ErrorCode.VALIDATION_ERROR, fieldErrors);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException e) {
        return buildValidationErrorResponse(e.getErrorCode(), Map.of());
    }

    private ResponseEntity<Map<String, Object>> buildValidationErrorResponse(
            ErrorCode errorCode,
            Map<String, String> fieldErrors) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", errorCode.getCode());
        response.put("message", errorCode.getMessage());
        response.put("fields", fieldErrors);
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
