package main.givelunch.services.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.FoodDto;
import main.givelunch.entities.Food;
import main.givelunch.exception.FoodNotFoundException;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final FoodRepository foodRepository;
    private final NutritionRepository nutritionRepository;

    public List<FoodDto> loadFoods(){
        return foodRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(FoodDto::from)
                .toList();
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
