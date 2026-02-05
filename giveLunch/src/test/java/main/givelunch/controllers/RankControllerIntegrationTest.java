package main.givelunch.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import main.givelunch.dto.rankDto.RankEntryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RankControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private main.givelunch.services.roulette.RankService rankService;

    @Test
    @WithMockUser
    @DisplayName("POST /api/ranks: 공백 제거 후 랭크를 증가시켜 반환")
    void recordRankReturnsIncrementedRank() throws Exception {
        when(rankService.increment(eq("비빔밥"))).thenReturn(new RankEntryDto("비빔밥", 3L));

        mockMvc.perform(post("/api/ranks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  비빔밥  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("비빔밥"))
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/ranks: 이름 누락 시 400 반환")
    void recordRankReturnsBadRequestWhenNameMissing() throws Exception {
        mockMvc.perform(post("/api/ranks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ranks/top: limit 범위를 1~5로 보정해 랭킹 반환")
    void topRanksAppliesSafeLimitAndReturnsRanks() throws Exception {
        when(rankService.getTopRanks(eq(5))).thenReturn(List.of(
                new RankEntryDto("비빔밥", 10L),
                new RankEntryDto("김치찌개", 7L)
        ));

        mockMvc.perform(get("/api/ranks/top").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("비빔밥"))
                .andExpect(jsonPath("$[0].count").value(10))
                .andExpect(jsonPath("$[1].name").value("김치찌개"));
    }


    @Test
    @WithMockUser
    @DisplayName("GET /api/ranks/top: limit가 0이면 1로 보정")
    void topRanksClampsLimitToOne() throws Exception {
        when(rankService.getTopRanks(eq(1))).thenReturn(List.of(new RankEntryDto("비빔밥", 1L)));

        mockMvc.perform(get("/api/ranks/top").param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("비빔밥"));

        verify(rankService).getTopRanks(eq(1));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/ranks/top: limit 미지정이면 기본값 5 사용")
    void topRanksUsesDefaultLimitWhenMissing() throws Exception {
        when(rankService.getTopRanks(eq(5))).thenReturn(List.of(new RankEntryDto("김치찌개", 2L)));

        mockMvc.perform(get("/api/ranks/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("김치찌개"));

        verify(rankService).getTopRanks(eq(5));
    }

}