package main.givelunch.services.agent;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.agent.AgentFoodImportResponse;
import main.givelunch.dto.agent.AgentFoodImportResult;
import main.givelunch.exception.FoodNotFoundException;
import main.givelunch.services.roulette.FoodSearchService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AgentFoodBulkImportService {
    private static final int DEFAULT_LIMIT_PER_NAME = 1;
    private static final String INVALID_NAME_MESSAGE = "음식 이름은 비어 있을 수 없습니다.";

    private final FoodSearchService foodSearchService;
    private final AgentFoodImportService agentFoodImportService;

    public AgentFoodImportResponse bulkImportFoods(List<String> names, Integer limitPerName) {
        int fetchCount = resolveFetchCount(limitPerName);
        List<BulkImportSegment> segments = new ArrayList<>();
        List<FoodAndNutritionDto> itemsToSave = new ArrayList<>();

        for (String name : names) {
            String normalizedName = normalizeName(name);
            if (normalizedName == null) {
                segments.add(BulkImportSegment.immediate(AgentFoodImportResult.failed(name, INVALID_NAME_MESSAGE)));
                continue;
            }

            List<FoodAndNutritionDto> externalFoods;
            try {
                externalFoods = foodSearchService.searchExternalFoods(normalizedName, fetchCount);
            } catch (RuntimeException e) {
                segments.add(BulkImportSegment.immediate(AgentFoodImportResult.failed(normalizedName, e.getMessage())));
                continue;
            }

            if (externalFoods.isEmpty()) {
                segments.add(BulkImportSegment.immediate(
                        AgentFoodImportResult.failed(normalizedName, new FoodNotFoundException(normalizedName).getMessage())
                ));
                continue;
            }

            itemsToSave.addAll(externalFoods);
            segments.add(BulkImportSegment.pending(externalFoods.size()));
        }

        if (itemsToSave.isEmpty()) {
            return AgentFoodImportResponse.from(collectImmediateResults(segments));
        }

        AgentFoodImportResponse saveResponse = agentFoodImportService.saveFoods(itemsToSave);
        return AgentFoodImportResponse.from(mergeResults(segments, saveResponse.results()));
    }

    private List<AgentFoodImportResult> collectImmediateResults(List<BulkImportSegment> segments) {
        List<AgentFoodImportResult> results = new ArrayList<>();
        for (BulkImportSegment segment : segments) {
            results.addAll(segment.immediateResults());
        }
        return results;
    }

    private List<AgentFoodImportResult> mergeResults(
            List<BulkImportSegment> segments,
            List<AgentFoodImportResult> savedResults
    ) {
        List<AgentFoodImportResult> mergedResults = new ArrayList<>();
        int savedResultIndex = 0;

        for (BulkImportSegment segment : segments) {
            if (!segment.immediateResults().isEmpty()) {
                mergedResults.addAll(segment.immediateResults());
                continue;
            }

            int nextIndex = savedResultIndex + segment.pendingSaveCount();
            if (nextIndex > savedResults.size()) {
                throw new IllegalStateException("bulk import save result count mismatch");
            }
            mergedResults.addAll(savedResults.subList(savedResultIndex, nextIndex));
            savedResultIndex = nextIndex;
        }

        if (savedResultIndex != savedResults.size()) {
            throw new IllegalStateException("bulk import save result count mismatch");
        }
        return mergedResults;
    }

    private int resolveFetchCount(Integer limitPerName) {
        return limitPerName == null ? DEFAULT_LIMIT_PER_NAME : limitPerName;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private record BulkImportSegment(
            List<AgentFoodImportResult> immediateResults,
            int pendingSaveCount
    ) {
        private static BulkImportSegment immediate(AgentFoodImportResult result) {
            return new BulkImportSegment(List.of(result), 0);
        }

        private static BulkImportSegment pending(int pendingSaveCount) {
            return new BulkImportSegment(List.of(), pendingSaveCount);
        }
    }
}
