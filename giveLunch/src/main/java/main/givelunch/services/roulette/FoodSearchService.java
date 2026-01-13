package main.givelunch.services.roulette;

import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodSearchResponseDto;
import main.givelunch.repositories.FoodRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodSearchService {
    private final FoodRepository foodRepository;

    public Long getIdByname(String name){
        // <--!db에 결과가 없는 경우는 추후에 처리-->

        return foodRepository
                .findIdByNameContaining(name, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
