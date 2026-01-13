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

    //keyword가 들어간 상위 10개 항목 id,name,category 반환
    @Transactional(readOnly = true)
    public List<FoodSearchResponseDto> searchByKeyword(String keyword) {
        String k = keyword == null ? "" : keyword.trim();
        if (k.isEmpty()) return List.of();

        return foodRepository.findTop10ByNameContaining(k).stream()
                .map(FoodSearchResponseDto::from)
                .toList();
    }

    public Long getIdByname(String name){
        String n = (name==null) ? "" : name.trim();
        if(n.isEmpty()){
            throw new IllegalArgumentException("Name cannot be empty");
        };

        return foodRepository.findIdByNameContaining(name);
    }
}
