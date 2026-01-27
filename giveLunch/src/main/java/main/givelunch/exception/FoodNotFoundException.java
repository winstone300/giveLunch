package main.givelunch.exception;

import lombok.Getter;

@Getter
public class FoodNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public FoodNotFoundException(Long foodId) {
        super(ErrorCode.FOOD_NOT_FOUND.getMessage() + " ID: " + foodId);
        this.errorCode = ErrorCode.FOOD_NOT_FOUND;
    }

    public FoodNotFoundException(String foodName) {
        super(foodName + " " + ErrorCode.FOOD_NOT_FOUND.getMessage());
        this.errorCode = ErrorCode.FOOD_NOT_FOUND;
    }
}