package main.givelunch.entities;

import java.math.BigDecimal;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FoodTest {

    @Test
    @DisplayName("FoodAndNutritionDto로 Food 생성 시 데이터가 올바르게 매핑")
    void createFoodFromDto() {
        // given
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setName("김치찌개");
        dto.setCategory("한식");
        dto.setServingSizeG(500);

        // when
        Food food = Food.from(dto);

        // then
        assertThat(food.getName()).isEqualTo("김치찌개");
        assertThat(food.getCategory()).isEqualTo("한식");
        assertThat(food.getServingSizeG()).isEqualTo(500);
    }

    @Test
    @DisplayName("영양 정보 설정 시 서로 연결 되는지")
    void setNutritionBiDirectional() {
        // given
        Food food = new Food();
        Nutrition nutrition = new Nutrition();

        // when
        food.setNutrition(nutrition);

        // then
        assertThat(food.getNutrition()).isEqualTo(nutrition);
        assertThat(nutrition.getFood()).isEqualTo(food);
    }

    @Test
    @DisplayName("음식 정보 업데이트 시 이름과 카테고리가 변경")
    void updateFood() {
        // given
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setName("김치찌개");
        dto.setCategory("한식");
        dto.setServingSizeG(500);

        Food food = Food.from(dto);

        FoodAndNutritionDto updateDto = new FoodAndNutritionDto();
        updateDto.setName("참치 김치찌개");
        updateDto.setCategory("가정식");
        updateDto.setServingSizeG(600);

        // when
        food.updateFood(updateDto);

        // then
        assertThat(food.getName()).isEqualTo("참치 김치찌개");
        assertThat(food.getCategory()).isEqualTo("가정식");
        assertThat(food.getServingSizeG()).isEqualTo(600);
    }

    @Test
    @DisplayName("업데이트 시 이름이 비어있으면 예외가 발생한다")
    void updateFoodValidation() {
        // given
        Food food = new Food();
        FoodAndNutritionDto invalidDto = new FoodAndNutritionDto();
        invalidDto.setName(""); // 빈 이름

        // when & then
        assertThatThrownBy(() -> food.updateFood(invalidDto))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getErrorCode()).isEqualTo(ErrorCode.INVALID_FOOD_NAME);
                })
                .hasMessage(ErrorCode.INVALID_FOOD_NAME.getMessage());
    }

    @Test
    @DisplayName("기존 영양정보가 없을 때 업데이트를 하면 새로운 Nutrition이 생성된다")
    void updateNutritionCreateNew() {
        // given
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setName("김치찌개");
        dto.setCategory("한식");
        dto.setServingSizeG(500);

        Food food = Food.from(dto);

        FoodAndNutritionDto updateDto = new FoodAndNutritionDto();
        updateDto.setName("김치찌개");

        NutritionDto nutritionDto = new NutritionDto();
        nutritionDto.setCalories(BigDecimal.valueOf(100.0));
        updateDto.setNutrition(nutritionDto);

        // when
        food.updateFood(updateDto);

        // then
        assertThat(food.getNutrition()).isNotNull();
        assertThat(food.getNutrition().getCalories()).isEqualTo(BigDecimal.valueOf(100.0));
        assertThat(food.getNutrition().getFood()).isEqualTo(food);
    }

    @Test
    @DisplayName("기존 영양 정보가 있을 때, 새로운 정보로 내용만 업데이트 되는지")
    void updateExistingNutrition() {
        // given
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setName("기존 음식");
        dto.setCategory("한식");
        dto.setServingSizeG(500);


        NutritionDto oldNutritionDto = new NutritionDto();
        oldNutritionDto.setCalories(BigDecimal.valueOf(100.0));
        dto.setNutrition(oldNutritionDto);

        Food food = Food.from(dto);

        Nutrition oldNutrition = food.getNutrition();

        // 2. 업데이트할 데이터 준비
        FoodAndNutritionDto updateDto = new FoodAndNutritionDto();
        updateDto.setName("기존 음식");

        NutritionDto newNutritionDto = new NutritionDto();
        newNutritionDto.setCalories(BigDecimal.valueOf(500.0)); // 변경할 값
        newNutritionDto.setCarbohydrate(BigDecimal.valueOf(50.0)); // 변경할 값

        updateDto.setNutrition(newNutritionDto);

        // when
        food.updateFood(updateDto);

        // then
        assertThat(food.getNutrition().getCalories()).isEqualTo(BigDecimal.valueOf(500.0));
        assertThat(food.getNutrition().getCarbohydrate()).isEqualTo(BigDecimal.valueOf(50.0));

        // (JPA에서는 식별자가 같은 기존 엔티티를 재사용하는 것이 성능/관리에 유리함)
        assertThat(food.getNutrition()).isSameAs(oldNutrition);
    }
}