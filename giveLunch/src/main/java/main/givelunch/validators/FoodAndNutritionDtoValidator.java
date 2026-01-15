package main.givelunch.validators;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class FoodAndNutritionDtoValidator {
    private final NutritionDtoValidator nutritionDtoValidator;

    public void hasName(FoodAndNutritionDto foodAndNutritionDto) {
        if (!StringUtils.hasText(foodAndNutritionDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "음식 이름은 필수입니다.");
        }
    }

    public boolean hasNutrition(FoodAndNutritionDto foodAndNutritionDto) {
        NutritionDto nutritionDto = foodAndNutritionDto.getNutrition();

        return nutritionDto != null && nutritionDtoValidator.hasValues(nutritionDto);
    }
}
