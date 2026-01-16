package main.givelunch.services.roulette;

import lombok.RequiredArgsConstructor;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.validators.FoodNameValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FoodSearchService {
    private final FoodRepository foodRepository;
    private final FoodNameValidator foodNameValidator;

    public Long getIdByName(String name){
        // <--!db에 결과가 없는 경우는 추후에 처리-->
        String normalized = (name == null) ? null : name.trim();
        if(!foodNameValidator.isValid(normalized)) return null;

        return foodRepository
                .findIdByNameContaining(normalized, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
