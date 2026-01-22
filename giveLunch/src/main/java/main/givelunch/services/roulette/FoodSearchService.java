package main.givelunch.services.roulette;

import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.properties.DataGoKrProperties;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import main.givelunch.services.external.DataGoKrFoodClient;
import main.givelunch.validators.FoodNameValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodSearchService {
    private final FoodRepository foodRepository;
    private final FoodNameValidator foodNameValidator;
    private final DataGoKrFoodClient dataGoKrFoodClient;
    private final DataGoKrProperties properties;

    /**
     * 음식 이름으로 ID를 조회하되, 로컬에 없으면 외부 API에서 조회 후 저장한다.
     */
    @Transactional
    public Long getIdByName(String name){
        String normalized = (name == null) ? null : name.trim();
        if(!foodNameValidator.isValid(normalized)) return null;

        // db에 없으면 null 값 반환
        Long existingId = foodRepository
                .findIdByNameContaining(normalized, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);

        return existingId;
    }

    public List<FoodAndNutritionDto> searchExternalFoods(String name, UserDetails user) {
        int fetchCount = isAdmin(user) ? properties.numOfRowsAdmin() : properties.numOfRowsUser();

        return dataGoKrFoodClient.fetchFoodsByName(name, fetchCount);
    }

    private boolean isAdmin(UserDetails user) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
