package main.givelunch.services.admin;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.FoodAndNutritionDto.FoodDto;
import main.givelunch.entities.Food;
import main.givelunch.exception.FoodNotFoundException;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final FoodRepository foodRepository;
    private final NutritionRepository nutritionRepository;

    public Page<FoodDto> loadFoods(int page, int size, String keyword){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        String normalizedKeyword = keyword == null ? "" : keyword.trim();

        if (normalizedKeyword.isEmpty()) {
            return foodRepository.findAll(pageable)
                    .map(FoodDto::from);
        }

        return foodRepository.findByNameContainingIgnoreCase(normalizedKeyword, pageable)
                .map(FoodDto::from);
    }

    public void deleteFoodsAndNutritions(Long id){
        foodRepository.deleteById(id);
    }

    public Food saveFoodAndNutrition(FoodAndNutritionDto foodAndNutritionDto){
        Food food = Food.from(foodAndNutritionDto);
        foodRepository.save(food);
        return food;
    }

    @Transactional
    public void updateFoodAndNutrition(Long id,FoodAndNutritionDto foodAndNutritionDto){
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new FoodNotFoundException(id));
        food.updateFood(foodAndNutritionDto);
    }
}
