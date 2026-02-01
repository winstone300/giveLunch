package main.givelunch.controllers;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import main.givelunch.dto.FoodSuggestionDto;
import main.givelunch.services.roulette.FoodSearchService;
import main.givelunch.services.roulette.MenuService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RouletteController.class)
@AutoConfigureMockMvc(addFilters = false)
class RouletteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private FoodSearchService foodSearchService;

    @Test
    @DisplayName("GET /api/menus/suggest: 메뉴 자동완성 결과를 반환")
    void suggestFoodsReturnsSuggestions() throws Exception {
        List<FoodSuggestionDto> suggestions = List.of(
                new FoodSuggestionDto(1L, "김치찌개", "https://example.com/kimchi.jpg"),
                new FoodSuggestionDto(2L, "김치볶음밥", null)
        );
        given(foodSearchService.suggestFoods("김치")).willReturn(suggestions);

        mockMvc.perform(get("/api/menus/suggest")
                        .param("query", "김치"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("김치찌개"))
                .andExpect(jsonPath("$[0].imgUrl").value("https://example.com/kimchi.jpg"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("김치볶음밥"))
                .andExpect(jsonPath("$[1].imgUrl").value(Matchers.nullValue()));
    }
}