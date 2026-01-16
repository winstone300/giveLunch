package main.givelunch.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import main.givelunch.dto.NutritionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NutritionDtoValidatorTest {

    private final NutritionDtoValidator nutritionDtoValidator = new NutritionDtoValidator();

    @Test
    @DisplayName("hasValues() - 모든 값이 null이면 false 반환")
    void hasValues_returnsFalse_whenAllNull() {
        NutritionDto dto = new NutritionDto();

        assertThat(nutritionDtoValidator.hasValues(dto)).isFalse();
    }

    @Test
    @DisplayName("hasValues() - 하나라도 값이 있으면 true 반환")
    void hasValues_returnsTrue_whenAnyValueExists() {
        NutritionDto dto = new NutritionDto();
        dto.setCalories(new BigDecimal("100.0"));

        assertThat(nutritionDtoValidator.hasValues(dto)).isTrue();
    }
}