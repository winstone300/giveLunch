package main.givelunch.services.roulette;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.exception.FoodNotFoundException;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodNutritionService {
    private final FoodRepository foodRepository;
    private final NutritionRepository nutritionRepository;

    @Transactional(readOnly = true)
    public FoodAndNutritionDto getFoodNutrition(Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new FoodNotFoundException(foodId));;

        // 영양정보가 안들어가 있으면 null
        Nutrition nutrition = nutritionRepository.findByFoodId(foodId).orElse(null);

        NutritionDto nutritionDto = (nutrition == null) ? null : NutritionDto.of(
                nutrition.getCalories(),
                nutrition.getProtein(),
                nutrition.getFat(),
                nutrition.getCarbohydrate()
        );

        return FoodAndNutritionDto.of(
                food.getId(),
                food.getName(),
                food.getCategory(),
                food.getImgUrl(),
                food.getServingSizeG(),
                nutritionDto,
                "INTERNAL_DB")
        ;
    }
}
