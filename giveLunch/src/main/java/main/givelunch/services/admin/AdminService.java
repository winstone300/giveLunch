package main.givelunch.services.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.FoodDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import main.givelunch.services.roulette.FoodNutritionService;
import main.givelunch.validators.FoodAndNutritionDtoValidator;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final FoodRepository foodRepository;
    private final NutritionRepository nutritionRepository;
    private final FoodAndNutritionDtoValidator foodAndNutritionDtoValidator;

    public List<FoodDto> loadFoods(){
        return foodRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream()
                .map(FoodDto::from)
                .toList();
    }

    public void deleteFoodsAndNutritions(Long id){
        deleteFoods(id);
        deleteNutritions(id);
    }

    public void deleteFoods(Long id){
        foodRepository.deleteById(id);
    }

    public void deleteNutritions(Long id){
        nutritionRepository.findByFoodId(id).ifPresent(nutritionRepository::delete);
    }

    public Food saveFood(FoodAndNutritionDto foodAndNutritionDto){
        foodAndNutritionDtoValidator.hasName(foodAndNutritionDto);
        Food food = Food.from(foodAndNutritionDto);
        foodRepository.save(food);
        return food;
    }

    public void saveNutrition(Food food,FoodAndNutritionDto foodAndNutritionDto){
        if(foodAndNutritionDtoValidator.hasNutrition(foodAndNutritionDto)) return;  // 조건 수정 필요
        Nutrition nutrition = Nutrition.from(food, foodAndNutritionDto);
        nutritionRepository.save(nutrition);
    }

    @Transactional
    public Food updateFood(Long id,FoodAndNutritionDto foodAndNutritionDto){
        Food food = foodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "음식을 찾을 수 없습니다."));
        food.updateFood(foodAndNutritionDto);
        return foodRepository.save(food);
    }

    @Transactional
    public void updateNutrition(Long id,Food food,FoodAndNutritionDto foodAndNutritionDto){
        boolean hasNutrition = foodAndNutritionDtoValidator.hasNutrition(foodAndNutritionDto);
        Nutrition nutrition = nutritionRepository.findByFoodId(id).orElse(null);
        if(hasNutrition){
            //기존에 정보가 있으면 update 아니면 새로 생성
            if(nutrition==null){
                nutrition = Nutrition.from(food,foodAndNutritionDto);
            }else{
                nutrition.updateNutrition(foodAndNutritionDto);
            }
            nutritionRepository.save(nutrition);
        }
    }

    @Transactional
    public void updateFoodAndNutrition(Long id,FoodAndNutritionDto foodAndNutritionDto){
        Food food = updateFood(id,foodAndNutritionDto);
        updateNutrition(id,food,foodAndNutritionDto);
    }
}
