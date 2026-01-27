package main.givelunch.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminFoodControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private NutritionRepository nutritionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/foods: 관리자 페이지에서 음식과 영양 정보를 등록")
    void createFoodAndNutritionPersistsData() throws Exception {
        // given
        String requestBody = buildFoodRequestJson("비빔밥", "한식", 450);

        // when
        mockMvc.perform(post("/api/admin/foods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // then
        List<Food> foods = foodRepository.findAll();
        assertThat(foods).hasSize(1);
        Food savedFood = foods.get(0);
        assertThat(savedFood.getName()).isEqualTo("비빔밥");
        assertThat(savedFood.getCategory()).isEqualTo("한식");

        Nutrition nutrition = nutritionRepository.findByFoodId(savedFood.getId()).orElseThrow();
        assertThat(nutrition.getCalories()).isEqualByComparingTo("450.00");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/admin/foods/{id}: 관리자가 음식과 영양 정보를 수정")
    void updateFoodAndNutritionUpdatesData() throws Exception {
        // given
        String createRequest = buildFoodRequestJson("라면", "분식", 500);
        String requestBody = buildFoodRequestJson("김치라면", "분식", 520);

        mockMvc.perform(post("/api/admin/foods")    // 음식 저장
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest))
                .andExpect(status().isCreated());

        Long foodId = foodRepository.findAll().get(0).getId();

        // when(음식 수정)
        mockMvc.perform(put("/api/admin/foods/{id}", foodId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNoContent());

        // then
        Food updatedFood = foodRepository.findById(foodId).orElseThrow();
        assertThat(updatedFood.getName()).isEqualTo("김치라면");
        assertThat(updatedFood.getServingSizeG()).isEqualTo(100);

        Nutrition updatedNutrition = nutritionRepository.findByFoodId(foodId).orElseThrow();
        assertThat(updatedNutrition.getCalories()).isEqualByComparingTo("520.00");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/admin/foods/{id}: 관리자가 음식과 영양 정보를 삭제")
    void deleteFoodAndNutritionRemovesData() throws Exception {
        // given
        String requestBody = buildFoodRequestJson("우동", "면", 300);

        mockMvc.perform(post("/api/admin/foods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        Long foodId = foodRepository.findAll().get(0).getId();

        // when
        mockMvc.perform(delete("/api/admin/foods/{id}", foodId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        // then
        entityManager.flush();
        entityManager.clear();
        assertThat(foodRepository.findById(foodId)).isEmpty();
        assertThat(nutritionRepository.findByFoodId(foodId)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/admin/foods: 일반 user 접근 차단")
    void createFoodForbiddenForUser() throws Exception {
        // given
        String requestBody = buildFoodRequestJson("비빔밥", "한식", 450);

        // when
        mockMvc.perform(post("/api/admin/foods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        // then
        assertThat(foodRepository.findAll()).isEmpty();
    }

    private String buildFoodRequestJson(String name, String category, int calories) {
        return "{" +
                "\"name\":\"" + name + "\"," +
                "\"category\":\"" + category + "\"," +
                "\"imgUrl\":\"http://example.com/food.png\"," +
                "\"servingSizeG\":100," +
                "\"nutrition\":{" +
                "\"calories\":" + calories + "," +
                "\"protein\":12.5," +
                "\"fat\":8.0," +
                "\"carbohydrate\":65.0" +
                "}," +
                "\"source\":\"test\"" +
                "}";
    }

    private FoodAndNutritionDto sampleFoodDto(String name, String category, int calories) {
        return FoodAndNutritionDto.of(
                null,
                name,
                category,
                "http://example.com/food.png",
                100,
                NutritionDto.of(
                        BigDecimal.valueOf(calories),
                        BigDecimal.valueOf(12.5),
                        BigDecimal.valueOf(8.0),
                        BigDecimal.valueOf(65.0)
                ),
                "test"
        );
    }
}
