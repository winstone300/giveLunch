package main.givelunch.services.roulette;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import main.givelunch.services.external.DataGoKrFoodClient;
import main.givelunch.validators.FoodNameValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodSearchService {
    private final FoodRepository foodRepository;
    private final FoodNameValidator foodNameValidator;
    private final NutritionRepository nutritionRepository;
    private final DataGoKrFoodClient dataGoKrFoodClient;

    /**
     * 음식 이름으로 ID를 조회하되, 로컬에 없으면 외부 API에서 조회 후 저장한다.
     */
    @Transactional
    public Long getIdByName(String name){
        // <--!db에 결과가 없는 경우는 추후에 처리-->
        String normalized = (name == null) ? null : name.trim();
        if(!foodNameValidator.isValid(normalized)) return null;

        // 1) 로컬 DB에서 먼저 조회
        Long existingId = foodRepository
                .findIdByNameContaining(normalized, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);

        if (existingId != null) {
            return existingId;
        }

        // 2) 로컬에 없으면 외부 API에서 조회 후 저장
        return dataGoKrFoodClient.fetchFoodByName(normalized)
                .map(this::saveExternalFood)
                .orElse(null);
    }

    /**
     * 외부 API에서 받은 결과를 로컬 DB에 저장하고 ID를 반환한다.
     */
    private Long saveExternalFood(FoodAndNutritionDto dto) {
        Food existing = foodRepository.findByName(dto.getName()).orElse(null);
        if (existing != null) {
            return existing.getId();
        }

        Food food = foodRepository.save(Food.from(dto));
        if (dto.getNutrition() != null) {
            Nutrition nutrition = Nutrition.from(food, dto);
            nutritionRepository.save(nutrition);
        }
        return food.getId();
    }
}
