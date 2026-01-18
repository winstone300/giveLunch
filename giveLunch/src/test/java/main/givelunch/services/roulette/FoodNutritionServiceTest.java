package main.givelunch.services.roulette;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FoodNutritionServiceTest {
    @Mock
    private FoodRepository foodRepository;

    @Mock
    private NutritionRepository nutritionRepository;

    @InjectMocks
    private FoodNutritionService foodNutritionService;

    @Test
    @DisplayName("getFoodNutrition() - food가 없으면 FOOD_NOT_FOUND 예외")
    void getFoodNutrition_throwsWhenFoodMissing() {
        // given
        Long foodId = 100L;
        when(foodRepository.findById(foodId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> foodNutritionService.getFoodNutrition(foodId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FOOD_NOT_FOUND: " + foodId);
    }

    @Test
    @DisplayName("getFoodNutrition() - nutrition이 없으면 nutrition에 null반환")
    void getFoodNutrition_throwsWhenNutritionMissing() {
        // given
        Long foodId = 200L;
        Food food = Mockito.mock(Food.class);

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(food));
        when(nutritionRepository.findByFoodId(foodId)).thenReturn(Optional.empty());

        // when
        FoodAndNutritionDto result = foodNutritionService.getFoodNutrition(foodId);

        // then
        assertThat(result.getNutrition()).isNull();
    }

    @Test
    @DisplayName("getFoodNutrition() - food와 nutrition이 있으면 DTO로 매핑해서 반환")
    void getFoodNutrition_returnsDtoWhenDataExists() {
        // given
        Long foodId = 10L;

        Food food = Mockito.mock(Food.class);
        Nutrition nutrition = Mockito.mock(Nutrition.class);

        when(food.getId()).thenReturn(foodId);
        when(food.getName()).thenReturn("샐러드");
        when(food.getCategory()).thenReturn("외식");
        when(food.getImgUrl()).thenReturn("http://img.test/salad");
        when(food.getServingSizeG()).thenReturn(150);

        when(nutrition.getCalories()).thenReturn(new BigDecimal("120.5"));
        when(nutrition.getProtein()).thenReturn(new BigDecimal("10.0"));
        when(nutrition.getFat()).thenReturn(new BigDecimal("4.0"));
        when(nutrition.getCarbohydrate()).thenReturn(new BigDecimal("15.0"));

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(food));
        when(nutritionRepository.findByFoodId(foodId)).thenReturn(Optional.of(nutrition));

        // when
        FoodAndNutritionDto result = foodNutritionService.getFoodNutrition(foodId);

        // then
        assertThat(result.getFoodId()).isEqualTo(foodId);
        assertThat(result.getName()).isEqualTo("샐러드");
        assertThat(result.getCategory()).isEqualTo("외식");
        assertThat(result.getImgUrl()).isEqualTo("http://img.test/salad");
        assertThat(result.getServingSizeG()).isEqualTo(150);

        assertThat(result.getNutrition().getCalories()).isEqualByComparingTo("120.5");
        assertThat(result.getNutrition().getProtein()).isEqualByComparingTo("10.0");
        assertThat(result.getNutrition().getFat()).isEqualByComparingTo("4.0");
        assertThat(result.getNutrition().getCarbohydrate()).isEqualByComparingTo("15.0");

        assertThat(result.getSource()).isEqualTo("INTERNAL_DB");
    }
}
