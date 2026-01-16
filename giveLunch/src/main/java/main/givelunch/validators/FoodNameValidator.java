package main.givelunch.validators;

import org.springframework.stereotype.Component;

@Component
public class FoodNameValidator {
    public boolean isValid(final String foodName) {
        return foodName != null && !foodName.isBlank();
    }
}
