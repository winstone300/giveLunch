package main.givelunch.services.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.FoodAndNutritionDto.NutritionDto;
import main.givelunch.dto.agent.AgentFoodImportResponse;
import main.givelunch.dto.agent.AgentFoodImportResultStatus;
import main.givelunch.entities.Food;
import main.givelunch.properties.DataGoKrProperties;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.services.admin.AdminService;
import main.givelunch.services.roulette.FoodSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentFoodImportServiceTest {

    @Mock
    private AdminService adminService;

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private FoodSearchService foodSearchService;

    private final DataGoKrProperties dataGoKrProperties = new DataGoKrProperties(
            "https://api.example.com",
            "service-key",
            "/foods",
            "json",
            1,
            8,
            3
    );

    private AgentFoodImportService agentFoodImportService;

    @BeforeEach
    void setUp() {
        agentFoodImportService = new AgentFoodImportService(
                adminService,
                foodRepository,
                foodSearchService,
                dataGoKrProperties
        );
    }

    @Test
    @DisplayName("saveFoods: 신규 음식은 저장 결과로 반환")
    void saveFoods_savesNewFood() {
        FoodAndNutritionDto item = sampleFood("비빔밥", 500);
        Food saved = Food.from(item);

        when(foodRepository.findIdByName("비빔밥")).thenReturn(Optional.empty());
        when(adminService.saveFoodAndNutrition(item)).thenReturn(saved);

        AgentFoodImportResponse response = agentFoodImportService.saveFoods(List.of(item));

        assertThat(response.savedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isZero();
        assertThat(response.failedCount()).isZero();
        assertThat(response.results().get(0).status()).isEqualTo(AgentFoodImportResultStatus.SAVED);
        verify(adminService).saveFoodAndNutrition(item);
    }

    @Test
    @DisplayName("saveFoods: 동일 이름이 있으면 skip")
    void saveFoods_skipsDuplicateFood() {
        FoodAndNutritionDto item = sampleFood("김치찌개", 320);
        when(foodRepository.findIdByName("김치찌개")).thenReturn(Optional.of(99L));

        AgentFoodImportResponse response = agentFoodImportService.saveFoods(List.of(item));

        assertThat(response.savedCount()).isZero();
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.results().get(0).foodId()).isEqualTo(99L);
        assertThat(response.results().get(0).status()).isEqualTo(AgentFoodImportResultStatus.SKIPPED);
    }

    @Test
    @DisplayName("importFoods: limitPerName이 없으면 관리자 기본 fetch count 사용")
    void importFoods_usesDefaultAdminFetchCount() {
        FoodAndNutritionDto external = sampleFood("라면", 430);
        Food saved = Food.from(external);

        when(foodSearchService.searchExternalFoods("라면", 8)).thenReturn(List.of(external));
        when(foodRepository.findIdByName("라면")).thenReturn(Optional.empty());
        when(adminService.saveFoodAndNutrition(external)).thenReturn(saved);

        AgentFoodImportResponse response = agentFoodImportService.importFoods(List.of("라면"), null);

        assertThat(response.savedCount()).isEqualTo(1);
        verify(foodSearchService).searchExternalFoods("라면", 8);
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
                        BigDecimal.valueOf(12.5),
                        BigDecimal.valueOf(8.0),
                        BigDecimal.valueOf(45.0)
                ),
                "test"
        );
    }
}
