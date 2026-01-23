package main.givelunch.controllers.exceptionHandler;

import main.givelunch.dto.ErrorResponseDto;
import main.givelunch.exception.FoodNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Food data가 없을 때
    @ExceptionHandler(FoodNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> foodNotFound(FoodNotFoundException e) {
        ErrorResponseDto response = new ErrorResponseDto("FOOD_NOT_FOUND", e.getMessage());
        return ResponseEntity.status(404).body(response);
    }
}
