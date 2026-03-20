package main.givelunch.services.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.agent.AgentFoodImportResponse;
import main.givelunch.dto.agent.AgentFoodImportResult;
import main.givelunch.exception.FoodNotFoundException;
import main.givelunch.properties.DataGoKrProperties;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.services.admin.AdminService;
import main.givelunch.services.roulette.FoodSearchService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentFoodImportService {
    private static final String DUPLICATE_REASON = "이미 같은 이름의 음식이 존재합니다.";

    private final AdminService adminService;
    private final FoodRepository foodRepository;
    private final FoodSearchService foodSearchService;
    private final DataGoKrProperties dataGoKrProperties;

    @Transactional
    public AgentFoodImportResponse saveFoods(List<FoodAndNutritionDto> items) {
        List<AgentFoodImportResult> results = new ArrayList<>();
        for (FoodAndNutritionDto item : items) {
            results.add(saveSingle(item));
        }
        return AgentFoodImportResponse.from(results);
    }

    @Transactional
    public AgentFoodImportResponse importFoods(List<String> names, Integer limitPerName) {
        int fetchCount = resolveFetchCount(limitPerName);
        List<AgentFoodImportResult> results = new ArrayList<>();

        for (String name : names) {
            String normalizedName = normalizeName(name);
            if (normalizedName == null) {
                results.add(AgentFoodImportResult.failed(name, "음식 이름은 비어 있을 수 없습니다."));
                continue;
            }

            List<FoodAndNutritionDto> externalFoods;
            try {
                externalFoods = foodSearchService.searchExternalFoods(normalizedName, fetchCount);
            } catch (RuntimeException e) {
                results.add(AgentFoodImportResult.failed(normalizedName, e.getMessage()));
                continue;
            }

            if (externalFoods.isEmpty()) {
                results.add(AgentFoodImportResult.failed(normalizedName, new FoodNotFoundException(normalizedName).getMessage()));
                continue;
            }

            for (FoodAndNutritionDto externalFood : externalFoods) {
                results.add(saveSingle(externalFood));
            }
        }

        return AgentFoodImportResponse.from(results);
    }

    private AgentFoodImportResult saveSingle(FoodAndNutritionDto item) {
        String normalizedName = item == null ? null : normalizeName(item.name());
        if (normalizedName == null) {
            return AgentFoodImportResult.failed(null, "음식 이름은 비어 있을 수 없습니다.");
        }

        Optional<Long> existingId = foodRepository.findIdByName(normalizedName);
        if (existingId.isPresent()) {
            return AgentFoodImportResult.skipped(normalizedName, existingId.get(), DUPLICATE_REASON);
        }

        FoodAndNutritionDto normalizedItem = FoodAndNutritionDto.of(
                item.foodId(),
                normalizedName,
                item.category(),
                item.imgUrl(),
                item.servingSizeG(),
                item.nutrition(),
                item.source()
        );

        try {
            Long savedId = adminService.saveFoodAndNutrition(normalizedItem).getId();
            return AgentFoodImportResult.saved(normalizedName, savedId);
        } catch (DataIntegrityViolationException e) {
            Optional<Long> duplicateId = foodRepository.findIdByName(normalizedName);
            if (duplicateId.isPresent()) {
                return AgentFoodImportResult.skipped(normalizedName, duplicateId.get(), DUPLICATE_REASON);
            }
            return AgentFoodImportResult.failed(normalizedName, e.getMostSpecificCause().getMessage());
        } catch (RuntimeException e) {
            return AgentFoodImportResult.failed(normalizedName, e.getMessage());
        }
    }

    private int resolveFetchCount(Integer limitPerName) {
        if (limitPerName == null) {
            return dataGoKrProperties.numOfRowsAdmin();
        }
        return limitPerName;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
