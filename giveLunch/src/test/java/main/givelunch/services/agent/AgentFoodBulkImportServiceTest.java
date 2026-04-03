package main.givelunch.services.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.FoodAndNutritionDto.NutritionDto;
import main.givelunch.dto.agent.AgentFoodImportResponse;
import main.givelunch.dto.agent.AgentFoodImportResult;
import main.givelunch.dto.agent.AgentFoodImportResultStatus;
import main.givelunch.services.roulette.FoodSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentFoodBulkImportServiceTest {

    @Mock
    private FoodSearchService foodSearchService;

    @Mock
    private AgentFoodImportService agentFoodImportService;

    private AgentFoodBulkImportService agentFoodBulkImportService;

    @BeforeEach
    void setUp() {
        agentFoodBulkImportService = new AgentFoodBulkImportService(foodSearchService, agentFoodImportService);
    }

    @Test
    @DisplayName("bulkImportFoods: 외부 조회 결과를 모아 기존 saveFoods를 한 번만 호출한다")
    void bulkImportFoods_callsSaveFoodsOnceWithCollectedItems() {
        FoodAndNutritionDto bibimbap = sampleFood("비빔밥", 500);
        FoodAndNutritionDto udon = sampleFood("우동", 280);
        AgentFoodImportResponse saveResponse = AgentFoodImportResponse.from(List.of(
                AgentFoodImportResult.saved("비빔밥", 1L),
                AgentFoodImportResult.skipped("우동", 2L, "이미 같은 이름의 음식이 존재합니다.")
        ));

        when(foodSearchService.searchExternalFoods("비빔밥", 1)).thenReturn(List.of(bibimbap));
        when(foodSearchService.searchExternalFoods("우동", 1)).thenReturn(List.of(udon));
        when(agentFoodImportService.saveFoods(List.of(bibimbap, udon))).thenReturn(saveResponse);

        AgentFoodImportResponse response = agentFoodBulkImportService.bulkImportFoods(
                List.of("비빔밥", " ", "우동"),
                null
        );

        assertThat(response.savedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(1);
        assertThat(response.results())
                .extracting(AgentFoodImportResult::status)
                .containsExactly(
                        AgentFoodImportResultStatus.SAVED,
                        AgentFoodImportResultStatus.FAILED,
                        AgentFoodImportResultStatus.SKIPPED
                );
        verify(agentFoodImportService).saveFoods(List.of(bibimbap, udon));
    }

    @Test
    @DisplayName("bulkImportFoods: limitPerName이 없으면 기본값 1을 사용한다")
    void bulkImportFoods_usesDefaultLimitPerName() {
        when(foodSearchService.searchExternalFoods("비빔밥", 1)).thenReturn(List.of());

        AgentFoodImportResponse response = agentFoodBulkImportService.bulkImportFoods(List.of("비빔밥"), null);

        assertThat(response.savedCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(1);
        verify(foodSearchService).searchExternalFoods("비빔밥", 1);
        verifyNoInteractions(agentFoodImportService);
    }

    @Test
    @DisplayName("bulkImportFoods: 조회 실패와 조회 결과 없음도 항목별 실패로 반환한다")
    void bulkImportFoods_returnsFailuresPerName() {
        when(foodSearchService.searchExternalFoods("라면", 2)).thenThrow(new RuntimeException("외부 API 오류"));
        when(foodSearchService.searchExternalFoods("김치찌개", 2)).thenReturn(List.of());

        AgentFoodImportResponse response = agentFoodBulkImportService.bulkImportFoods(
                List.of("라면", "김치찌개"),
                2
        );

        assertThat(response.savedCount()).isZero();
        assertThat(response.skippedCount()).isZero();
        assertThat(response.failedCount()).isEqualTo(2);
        assertThat(response.results())
                .extracting(AgentFoodImportResult::reason)
                .containsExactly("외부 API 오류", "김치찌개 해당 음식을 찾을 수 없습니다.");
        verifyNoInteractions(agentFoodImportService);
    }

    private FoodAndNutritionDto sampleFood(String name, int calories) {
        return FoodAndNutritionDto.of(
                null,
                name,
                "한식",
                "http://example.com/" + name + ".png",
                200,
                NutritionDto.of(
                        BigDecimal.valueOf(calories),
                        BigDecimal.valueOf(10.5),
                        BigDecimal.valueOf(8.0),
                        BigDecimal.valueOf(45.0)
                ),
                "test"
        );
    }
}
