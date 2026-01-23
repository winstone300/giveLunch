package main.givelunch;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /login: 익명 사용자는 로그인 페이지에 접근 가능")
    void loginPageAccessibleForAnonymous() throws Exception {
        // when & then
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/login"));
    }

    @Test
    @DisplayName("GET /roulette: 익명 사용자 룰렛 페이지 접근 가능")
    void rouletteRedirectsToLoginForAnonymous() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/roulette"))
                .andExpect(status().isOk())
                .andExpect(view().name("roulette/roulette"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin: admin 관리자 페이지 접속가능")
    void adminPageAccessibleForAdmin() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dataBase.html"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /admin: 일반 user 관리자 페이지 접근 차단")
    void adminPageForbiddenForUser() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }
}