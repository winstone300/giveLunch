package main.givelunch.validators;

import main.givelunch.dto.NutritionDto;
import org.springframework.stereotype.Component;

@Component
public class NutritionDtoValidator {
    public boolean hasValues(NutritionDto nDto) {
        return nDto.getCalories() != null || nDto.getProtein() != null || nDto.getCarbohydrate() != null || nDto.getFat() != null;
    }
}
