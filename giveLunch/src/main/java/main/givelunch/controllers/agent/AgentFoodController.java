package main.givelunch.controllers.agent;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.agent.AgentExternalFoodSearchRequest;
import main.givelunch.dto.agent.AgentFoodImportRequest;
import main.givelunch.dto.agent.AgentFoodImportResponse;
import main.givelunch.dto.agent.AgentFoodSaveRequest;
import main.givelunch.properties.DataGoKrProperties;
import main.givelunch.services.agent.AgentFoodImportService;
import main.givelunch.services.roulette.FoodSearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent/foods")
public class AgentFoodController {
    private final FoodSearchService foodSearchService;
    private final AgentFoodImportService agentFoodImportService;
    private final DataGoKrProperties dataGoKrProperties;

    @Operation(summary = "에이전트 외부 음식 검색", description = "고정 API Key 인증으로 외부 음식 정보를 검색")
    @PostMapping("/search-external")
    public List<FoodAndNutritionDto> searchExternalFoods(@Valid @RequestBody AgentExternalFoodSearchRequest request) {
        int fetchCount = request.limit() == null ? dataGoKrProperties.numOfRowsAdmin() : request.limit();
        return foodSearchService.searchExternalFoods(request.name(), fetchCount);
    }

    @Operation(summary = "에이전트 음식 저장", description = "전달받은 음식 및 영양 정보를 DB에 저장")
    @PostMapping("/save")
    public AgentFoodImportResponse saveFoods(@Valid @RequestBody AgentFoodSaveRequest request) {
        return agentFoodImportService.saveFoods(request.items());
    }

    @Operation(summary = "에이전트 음식 import", description = "이름 목록을 외부 검색 후 DB에 저장")
    @PostMapping("/import")
    public AgentFoodImportResponse importFoods(@Valid @RequestBody AgentFoodImportRequest request) {
        return agentFoodImportService.importFoods(request.names(), request.limitPerName());
    }
}
