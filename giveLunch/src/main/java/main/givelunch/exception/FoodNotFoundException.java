package main.givelunch.exception;

public class FoodNotFoundException extends RuntimeException {
    public FoodNotFoundException(Long foodId) {
        super("해당 음식을 찾을 수 없습니다. ID: " + foodId);
    }

    public FoodNotFoundException(String foodName) {
        super(foodName + " 음식을 찾을 수 없습니다.");
    }
}