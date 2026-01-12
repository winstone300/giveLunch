package main.givelunch.services.roulette;

import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodSearchResponseDto;
import main.givelunch.repositories.FoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodSearchService {
    private final FoodRepository foodRepository;

    @Transactional(readOnly = true)
    public List<FoodSearchResponseDto> searchByKeyword(String keyword) {
        String k = keyword == null ? "" : keyword.trim();
        if (k.isEmpty()) return List.of();

        return foodRepository.findTop10ByNameContaining(k).stream()
                .map(FoodSearchResponseDto::from)
                .toList();
    }
}
