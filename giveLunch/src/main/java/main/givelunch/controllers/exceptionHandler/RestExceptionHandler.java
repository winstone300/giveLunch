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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice(annotations = RestController.class)
public class RestExceptionHandler {
    // Food data가 없을 때
    @ExceptionHandler(FoodNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> foodNotFound(FoodNotFoundException e) {
        return buildErrorResponse(e.getErrorCode());
    }
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

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(ValidationException e) {
        return buildErrorResponse(e.getErrorCode(),e.getMessage());
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(ErrorCode errorCode) {
        ErrorResponseDto response = new ErrorResponseDto(errorCode.getCode(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(ErrorCode errorCode, String message) {
        ErrorResponseDto response = new ErrorResponseDto(errorCode.getCode(), message);
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
