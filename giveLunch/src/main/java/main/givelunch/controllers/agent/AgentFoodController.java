package main.givelunch.controllers.agent;

import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(
        name = "Agent Food API",
        description = "MCP food tools가 사용하는 고정 API Key 기반 음식 검색/저장/import API"
)
public class AgentFoodController {
    private final FoodSearchService foodSearchService;
    private final AgentFoodImportService agentFoodImportService;
    private final DataGoKrProperties dataGoKrProperties;

    @Operation(
            summary = "에이전트 외부 음식 검색",
            description = "MCP의 search_external_foods 도구가 사용하는 엔드포인트"
                    + "limit가 없으면 관리자 기본 조회 건수를 사용"
    )
    @PostMapping("/search-external")
    public List<FoodAndNutritionDto> searchExternalFoods(@Valid @RequestBody AgentExternalFoodSearchRequest request) {
        int fetchCount = request.limit() == null ? dataGoKrProperties.numOfRowsAdmin() : request.limit();
        return foodSearchService.searchExternalFoods(request.name(), fetchCount);
    }

    @Operation(
            summary = "에이전트 음식 저장",
            description = "MCP의 save_foods 도구가 사용하는 엔드포인트입니다. "
                    + "전달받은 음식/영양정보를 저장하고 결과별 상태와 집계 카운트를 반환합니다."
    )
    @PostMapping("/save")
    public AgentFoodImportResponse saveFoods(@Valid @RequestBody AgentFoodSaveRequest request) {
        return agentFoodImportService.saveFoods(request.items());
    }

    @Operation(
            summary = "에이전트 음식 import",
            description = "MCP의 import_foods_by_name 도구가 사용하는 엔드포인트입니다. "
                    + "names의 각 항목에 대해 외부 검색 후 DB에 저장하며, limitPerName은 이름당 조회할 외부 결과 수를 뜻합니다."
    )
    @PostMapping("/import")
    public AgentFoodImportResponse importFoods(@Valid @RequestBody AgentFoodImportRequest request) {
        return agentFoodImportService.importFoods(request.names(), request.limitPerName());
    }
}
