package main.givelunch.controllers.admin;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import main.givelunch.dto.rankDto.RankRebuildResultDto;
import main.givelunch.services.roulette.RankService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminRankControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RankService rankService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/admin/ranks/rebuild: 관리자 요청 시 복구 결과 반환")
    void rebuildRanksReturnsResultForAdmin() throws Exception {
        when(rankService.rebuildRanks()).thenReturn(new RankRebuildResultDto(12L, 4L, 1_767_225_600L));

        mockMvc.perform(post("/api/admin/ranks/rebuild").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rebuiltEventCount").value(12))
                .andExpect(jsonPath("$.rebuiltFoodCount").value(4))
                .andExpect(jsonPath("$.cutoffEpochSeconds").value(1767225600L));

        verify(rankService).rebuildRanks();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/admin/ranks/rebuild: 일반 사용자 접근 차단")
    void rebuildRanksForbiddenForUser() throws Exception {
        mockMvc.perform(post("/api/admin/ranks/rebuild").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
