package main.givelunch.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FoodAndNutritionDtoValidatorTest {

    @Mock
    private NutritionDtoValidator nutritionDtoValidator;

    @InjectMocks
    private FoodAndNutritionDtoValidator foodAndNutritionDtoValidator;

    @Test
    @DisplayName("hasName() - 이름이 없으면 BAD_REQUEST")
    void hasName_throwsWhenNameMissing() {
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setName("  ");

        assertThatThrownBy(() -> foodAndNutritionDtoValidator.hasName(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException response = (ResponseStatusException) exception;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getReason()).isEqualTo("음식 이름은 필수입니다.");
                });
    }

    @Test
    @DisplayName("hasNutrition() - nutrition이 null이면 false")
    void hasNutrition_returnsFalse_whenNutritionIsNull() {
        FoodAndNutritionDto dto = new FoodAndNutritionDto();

        assertThat(foodAndNutritionDtoValidator.hasNutrition(dto)).isFalse();
    }

    @Test
    @DisplayName("hasNutrition() - nutrition은 있지만 값이 없으면 false")
    void hasNutrition_returnsFalse_whenNutritionHasNoValues() {
        NutritionDto nutritionDto = new NutritionDto();
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setNutrition(nutritionDto);

        when(nutritionDtoValidator.hasValues(nutritionDto)).thenReturn(false);

        assertThat(foodAndNutritionDtoValidator.hasNutrition(dto)).isFalse();
    }

    @Test
    @DisplayName("hasNutrition() - nutrition에 값이 있으면 true")
    void hasNutrition_returnsTrue_whenNutritionHasValues() {
        NutritionDto nutritionDto = new NutritionDto();
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setNutrition(nutritionDto);

        when(nutritionDtoValidator.hasValues(nutritionDto)).thenReturn(true);

        assertThat(foodAndNutritionDtoValidator.hasNutrition(dto)).isTrue();
    }
}