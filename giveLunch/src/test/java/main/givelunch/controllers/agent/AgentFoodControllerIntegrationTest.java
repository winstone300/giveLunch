package main.givelunch.controllers.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.FoodAndNutritionDto.NutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.services.external.DataGoKrFoodClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgentFoodControllerIntegrationTest {
    private static final String AUTHORIZATION = "Authorization";
    private static final String API_KEY = "Bearer test-agent-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FoodRepository foodRepository;

    @MockitoBean
    private DataGoKrFoodClient dataGoKrFoodClient;

    @Test
    @DisplayName("POST /api/agent/foods/search-external: API key 없으면 401")
    void searchExternalFoods_requiresApiKey() throws Exception {
        mockMvc.perform(post("/api/agent/foods/search-external")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"비빔밥","limit":2}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AGENT_API_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /api/agent/foods/search-external: 유효한 API key로 외부 검색")
    void searchExternalFoods_returnsResultsForValidApiKey() throws Exception {
        FoodAndNutritionDto externalDto = sampleFood("비빔밥", 510);
        when(dataGoKrFoodClient.fetchFoodsByName(eq("비빔밥"), eq(2))).thenReturn(List.of(externalDto));

        mockMvc.perform(post("/api/agent/foods/search-external")
                        .header(AUTHORIZATION, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"비빔밥","limit":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("비빔밥"))
                .andExpect(jsonPath("$[0].nutrition.calories").value(510));
    }

    @Test
    @DisplayName("POST /api/agent/foods/save: 신규 저장과 중복 skip을 함께 반환")
    void saveFoods_returnsSavedAndSkipped() throws Exception {
        foodRepository.save(Food.from(sampleFood("김치찌개", 320)));

        mockMvc.perform(post("/api/agent/foods/save")
                        .header(AUTHORIZATION, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "name": "김치찌개",
                                      "category": "한식",
                                      "imgUrl": "http://example.com/kimchi.png",
                                      "servingSizeG": 200,
                                      "nutrition": {
                                        "calories": 320,
                                        "protein": 10.5,
                                        "fat": 8.0,
                                        "carbohydrate": 45.0
                                      },
                                      "source": "test"
                                    },
                                    {
                                      "name": "된장찌개",
                                      "category": "한식",
                                      "imgUrl": "http://example.com/doenjang.png",
                                      "servingSizeG": 180,
                                      "nutrition": {
                                        "calories": 280,
                                        "protein": 11.0,
                                        "fat": 7.0,
                                        "carbohydrate": 32.0
                                      },
                                      "source": "test"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.results[0].status").value("SKIPPED"))
                .andExpect(jsonPath("$.results[1].status").value("SAVED"));

        assertThat(foodRepository.findByName("된장찌개")).isPresent();
    }

    @Test
    @DisplayName("POST /api/agent/foods/import: 외부 검색 후 저장 결과를 반환")
    void importFoods_searchesAndPersistsFoods() throws Exception {
        when(dataGoKrFoodClient.fetchFoodsByName(eq("우동"), eq(3)))
                .thenReturn(List.of(sampleFood("우동", 280)));

        mockMvc.perform(post("/api/agent/foods/import")
                        .header(AUTHORIZATION, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"names":["우동"],"limitPerName":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.results[0].name").value("우동"));

        assertThat(foodRepository.findByName("우동")).isPresent();
    }

    @Test
    @DisplayName("POST /api/agent/foods/bulk-import: 여러 이름을 한 번에 적재하고 기본 limit 1을 사용")
    void bulkImportFoods_searchesAndPersistsFoodsInOneRequest() throws Exception {
        foodRepository.save(Food.from(sampleFood("비빔밥", 510)));
        when(dataGoKrFoodClient.fetchFoodsByName(eq("된장찌개"), eq(1)))
                .thenReturn(List.of(sampleFood("된장찌개", 280)));
        when(dataGoKrFoodClient.fetchFoodsByName(eq("비빔밥"), eq(1)))
                .thenReturn(List.of(sampleFood("비빔밥", 510)));
        when(dataGoKrFoodClient.fetchFoodsByName(eq("김치찌개"), eq(1)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/agent/foods/bulk-import")
                        .header(AUTHORIZATION, API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"names":["된장찌개"," ","비빔밥","김치찌개"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.results[0].name").value("된장찌개"))
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.results[1].status").value("FAILED"))
                .andExpect(jsonPath("$.results[2].name").value("비빔밥"))
                .andExpect(jsonPath("$.results[2].status").value("SKIPPED"))
                .andExpect(jsonPath("$.results[3].name").value("김치찌개"))
                .andExpect(jsonPath("$.results[3].status").value("FAILED"));

        assertThat(foodRepository.findByName("된장찌개")).isPresent();
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
