package main.givelunch.controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
    @DisplayName("GET /api/admin/foods: 관리자 조회 시 페이지 형태의 음식 목록 반환")
    void loadFoodsReturnsPagedListForAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/foods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFoodRequestJson("비빔밥", "한식", 450)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/foods")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("비빔밥"))
                .andExpect(jsonPath("$.content[0].category").value("한식"))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/foods: keyword 검색 시 이름 포함 항목만 반환")
    void loadFoodsWithKeywordReturnsFilteredResult() throws Exception {
        mockMvc.perform(post("/api/admin/foods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFoodRequestJson("김치찌개", "한식", 450)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/foods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFoodRequestJson("파스타", "양식", 550)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/foods")
                        .param("page", "0")
                        .param("size", "10")
                        .param("keyword", "김치"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("김치찌개"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /api/admin/foods: 일반 user 조회 접근 차단")
    void loadFoodsForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/admin/foods"))
                .andExpect(status().isForbidden());
    }

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
    @DisplayName("POST /api/admin/foods/import/preview: CSV 파일을 미리보기로 파싱")
    void previewFoodImportParsesCsv() throws Exception {
        String csv = "name,category,calories,carbohydrate,protein,fat\n"
                + "김밥,분식,350,55,12,8\n"
                + ",한식,120,20,5,2\n";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "foods.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/admin/foods/import/preview")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validCount").value(1))
                .andExpect(jsonPath("$.invalidCount").value(1))
                .andExpect(jsonPath("$.rows[0].rowNumber").value(1))
                .andExpect(jsonPath("$.rows[1].rowNumber").value(2))
                .andExpect(jsonPath("$.rows[0].name").value("김밥"))
                .andExpect(jsonPath("$.rows[0].category").value("기타"))
                .andExpect(jsonPath("$.rows[1].valid").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/foods/import: 신규 음식은 저장하고 기존 음식은 skip")
    void importFoodsPersistsNewRowsAndSkipsDuplicateNames() throws Exception {
        mockMvc.perform(post("/api/admin/foods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFoodRequestJson("기존음식", "한식", 450)))
                .andExpect(status().isCreated());

        String importRequest = """
                {
                  "items": [
                    {"rowNumber":1,"name":"신규음식","category":"양식","imgUrl":null,"servingSizeG":200,"nutrition":{"calories":500,"protein":10,"fat":5,"carbohydrate":80}},
                    {"rowNumber":2,"name":"기존음식","category":"한식","imgUrl":null,"servingSizeG":100,"nutrition":null},
                    {"rowNumber":3,"name":" ","category":"일식","imgUrl":null,"servingSizeG":100,"nutrition":null}
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/foods/import")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.results[0].status").value("SAVED"))
                .andExpect(jsonPath("$.results[1].status").value("SKIPPED"))
                .andExpect(jsonPath("$.results[2].status").value("FAILED"));

        assertThat(foodRepository.findByName("신규음식")).isPresent();
        assertThat(foodRepository.findByName("기존음식")).isPresent();
        assertThat(foodRepository.findAll()).hasSize(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/foods/import: overwriteExisting=true이면 기존 음식을 덮어쓴다")
    void importFoodsOverwritesDuplicateNamesWhenRequested() throws Exception {
        mockMvc.perform(post("/api/admin/foods")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildFoodRequestJson("기존음식", "한식", 450)))
                .andExpect(status().isCreated());

        Long foodId = foodRepository.findByName("기존음식").orElseThrow().getId();
        String importRequest = """
                {
                  "overwriteExisting": true,
                  "items": [
                    {"rowNumber":1,"name":"기존음식","category":"양식","imgUrl":"https://img.example.com/new.png","servingSizeG":250,"nutrition":{"calories":700,"protein":30,"fat":20,"carbohydrate":90}}
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/foods/import")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(importRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(0))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.results[0].status").value("UPDATED"))
                .andExpect(jsonPath("$.results[0].foodId").value(foodId));

        entityManager.flush();
        entityManager.clear();
        Food updatedFood = foodRepository.findById(foodId).orElseThrow();
        assertThat(updatedFood.getCategory()).isEqualTo("양식");
        assertThat(updatedFood.getImgUrl()).isEqualTo("https://img.example.com/new.png");
        assertThat(updatedFood.getServingSizeG()).isEqualTo(250);
        Nutrition updatedNutrition = nutritionRepository.findByFoodId(foodId).orElseThrow();
        assertThat(updatedNutrition.getCalories()).isEqualByComparingTo("700.00");
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
}
