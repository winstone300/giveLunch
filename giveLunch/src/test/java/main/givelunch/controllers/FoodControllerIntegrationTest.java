package main.givelunch.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FoodControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private NutritionRepository nutritionRepository;

    @Test
    @WithMockUser
    @DisplayName("GET /api/foods/{foodId}/nutrition: 음식과 영양 정보를 조회")
    void getFoodNutritionReturnsFoodAndNutrition() throws Exception {
        // given
        FoodAndNutritionDto request = sampleFoodDto("김치찌개", "한식", 320);
        Food food = foodRepository.save(Food.from(request));
        nutritionRepository.save(Nutrition.from(food, request));

        // when & then
        mockMvc.perform(get("/api/foods/{foodId}/nutrition", food.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foodId").value(food.getId()))
                .andExpect(jsonPath("$.name").value("김치찌개"))
                .andExpect(jsonPath("$.category").value("한식"))
                .andExpect(jsonPath("$.nutrition.calories").value(320))
                .andExpect(jsonPath("$.nutrition.protein").value(10.5))
                .andExpect(jsonPath("$.nutrition.fat").value(8.0))
                .andExpect(jsonPath("$.nutrition.carbohydrate").value(45.0));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/foods/getId: 음식 이름으로 ID를 조회")
    void getFoodIdReturnsIdForName() throws Exception {
        // given
        Food food = foodRepository.save(Food.from(sampleFoodDto("우동", "면", 280)));

        // when & then
        mockMvc.perform(get("/api/foods/getId").param("name", "우동"))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(food.getId())));
    }

    private FoodAndNutritionDto sampleFoodDto(String name, String category, int calories) {
        return FoodAndNutritionDto.of(
                null,
                name,
                category,
                "http://example.com/food.png",
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