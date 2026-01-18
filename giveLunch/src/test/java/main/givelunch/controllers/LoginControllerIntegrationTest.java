package main.givelunch.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import main.givelunch.entities.UserInfo;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("GET /login: 로그인 요청시 사전에 설정한 로그인 화면 보여줌")
    void loginPageRendersForAnonymous() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/login"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /signup: 회원가입 성공시 회원이 저장되고 로그인 페이지로 이동")
    void signupSuccessPersistsUser() throws Exception {
        // given
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("userName", "newuser")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("email", "newuser@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?success"));

        // then
        UserInfo savedUser = userRepository.findByUserName("newuser").orElseThrow();
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /signup: 유효성 실패 시 에러 메시지와 함께 signup 뷰를 반환")
    void signupValidationFailureReturnsSignupView() throws Exception {
        // given
        mockMvc.perform(post("/signup")
                        .with(csrf())
                        .param("userName", "")
                        .param("password", "password123")
                        .param("passwordConfirm", "password123")
                        .param("email", "invalid@example.com"))
                // when & then
                .andExpect(status().isOk())
                .andExpect(view().name("login/signup"))
                .andExpect(model().attribute("error", "아이디를 입력해주세요."));

        // then
        assertThat(userRepository.findByUserName("")).isEmpty();
    }
}