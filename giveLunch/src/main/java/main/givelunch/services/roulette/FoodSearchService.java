package main.givelunch.services.roulette;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.FoodAndNutritionDto.FoodSuggestionDto;
import main.givelunch.properties.DataGoKrProperties;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.services.external.DataGoKrFoodClient;
import main.givelunch.validators.NameValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodSearchService {
    private final FoodRepository foodRepository;
    private final NameValidator nameValidator;
    private final DataGoKrFoodClient dataGoKrFoodClient;
    private final DataGoKrProperties properties;

    /**
     * 음식 이름으로 ID를 조회하되, 로컬에 없으면 외부 API에서 조회 후 저장한다.
     */
    @Transactional
    public Long getIdByName(String name){
        String normalized = (name == null) ? null : name.trim();
        if(!nameValidator.isValid(normalized)) return null;

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

    public List<FoodSuggestionDto> suggestFoods(String name) {
        if (!nameValidator.isValid(name)) {
            return List.of();
        }
        return foodRepository
                .findByNameContainingOrderByShortestName(name.trim(), PageRequest.of(0, 10))
                .stream()
                .map(FoodSuggestionDto::from)
                .collect(Collectors.toList());
    }

    private boolean isAdmin(UserDetails user) {
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
