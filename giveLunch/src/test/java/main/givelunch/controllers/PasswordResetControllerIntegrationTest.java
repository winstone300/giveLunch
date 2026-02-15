package main.givelunch.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import main.givelunch.entities.PasswordResetToken;
import main.givelunch.entities.UserInfo;
import main.givelunch.model.Role;
import main.givelunch.properties.SecurityProperties;
import main.givelunch.repositories.PasswordResetTokenRepository;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser
    @DisplayName("GET /forgot-password, /reset-password: 재설정 관련 화면 렌더링")
    void resetViewsRender() throws Exception {
        mockMvc.perform(get("/forgot-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/forgot-password"));

        mockMvc.perform(get("/reset-password"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/reset-password"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /forgot-password: 정상 요청 시 재설정 코드 저장 후 reset 페이지로 리다이렉트")
    void forgotPasswordSavesTokenAndRedirects() throws Exception {
        userRepository.save(UserInfo.builder()
                .userName("resetuser")
                .password(passwordEncoder.encode("oldPassword"))
                .email("reset@example.com")
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("userName", "resetuser")
                        .param("email", "reset@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reset-password?remainingSeconds=600"));

        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
        PasswordResetToken token = passwordResetTokenRepository.findAll().get(0);
        assertThat(token.getEmail()).isEqualTo("reset@example.com");
    }

    @Test
    @WithMockUser
    @DisplayName("POST /forgot-password: 사용자 정보 불일치 시 안내 문구와 forgotPassword 뷰 반환")
    void forgotPasswordReturnsViewWhenUserNotFound() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("userName", "missing")
                        .param("email", "missing@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/forgot-password"))
                .andExpect(model().attribute("error", "아이디 또는 이메일이 올바르지 않습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password/verify: 코드 인증 성공 시 verified=true")
    void verifyResetCodeMarksAsVerified() throws Exception {
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .email("verify-reset@example.com")
                .code("123456")
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/reset-password/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"verify-reset@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("코드 인증이 완료되었습니다."));

        PasswordResetToken verified = passwordResetTokenRepository.findTopByEmailOrderByCreatedAtDesc("verify-reset@example.com")
                .orElseThrow();
        assertThat(verified.isVerified()).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password: 정상 요청 시 비밀번호 변경 후 로그인 페이지로 이동")
    void resetPasswordUpdatesPasswordAndMarksTokenUsed() throws Exception {
        userRepository.save(UserInfo.builder()
                .userName("resetuser")
                .password(passwordEncoder.encode("oldPassword"))
                .email("reset2@example.com")
                .role(Role.USER)
                .build());

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .email("reset2@example.com")
                .code("654321")
                .verified(true)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("email", "reset2@example.com")
                        .param("code", "654321")
                        .param("password", "newPassword")
                        .param("passwordConfirm", "newPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?resetSuccess"));

        UserInfo updated = userRepository.findByEmail("reset2@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("newPassword", updated.getPassword())).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password: 비밀번호 확인 불일치 시 resetPassword 뷰 반환")
    void resetPasswordMismatchReturnsResetView() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("email", "reset2@example.com")
                        .param("code", "654321")
                        .param("password", "newPassword")
                        .param("passwordConfirm", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/reset-password"))
                .andExpect(model().attribute("error", "비밀번호 확인이 일치하지 않습니다."));
    }


    @Test
    @WithMockUser
    @DisplayName("POST /forgot-password: 아이디 누락 시 안내 문구와 forgotPassword 뷰 반환")
    void forgotPasswordReturnsViewWhenUsernameBlank() throws Exception {
        mockMvc.perform(post("/forgot-password")
                        .with(csrf())
                        .param("userName", "")
                        .param("email", "reset@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/forgot-password"))
                .andExpect(model().attribute("error", "아이디 또는 이메일이 올바르지 않습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password: 코드 불일치 시 resetPassword 뷰와 에러 메시지 반환")
    void resetPasswordReturnsResetViewWhenCodeInvalid() throws Exception {
        userRepository.save(UserInfo.builder()
                .userName("resetuser3")
                .password(passwordEncoder.encode("oldPassword"))
                .email("reset3@example.com")
                .role(Role.USER)
                .build());

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .email("reset3@example.com")
                .code("111111")
                .verified(true)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("email", "reset3@example.com")
                        .param("code", "222222")
                        .param("password", "newPassword")
                        .param("passwordConfirm", "newPassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/reset-password"))
                .andExpect(model().attribute("error", "비밀번호 재설정 코드가 올바르지 않습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password: 코드 미인증 상태면 resetPassword 뷰와 안내 메시지 반환")
    void resetPasswordReturnsResetViewWhenCodeNotVerified() throws Exception {
        userRepository.save(UserInfo.builder()
                .userName("resetuser5")
                .password(passwordEncoder.encode("oldPassword"))
                .email("reset5@example.com")
                .role(Role.USER)
                .build());

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .email("reset5@example.com")
                .code("444444")
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("email", "reset5@example.com")
                        .param("code", "444444")
                        .param("password", "newPassword")
                        .param("passwordConfirm", "newPassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/reset-password"))
                .andExpect(model().attribute("error", "비밀번호 재설정 코드 인증을 완료해주세요."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password: 만료된 코드면 resetPassword 뷰와 만료 메시지 반환")
    void resetPasswordReturnsResetViewWhenCodeExpired() throws Exception {
        userRepository.save(UserInfo.builder()
                .userName("resetuser4")
                .password(passwordEncoder.encode("oldPassword"))
                .email("reset4@example.com")
                .role(Role.USER)
                .build());

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .email("reset4@example.com")
                .code("333333")
                .verified(true)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("email", "reset4@example.com")
                        .param("code", "333333")
                        .param("password", "newPassword")
                        .param("passwordConfirm", "newPassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("login/reset-password"))
                .andExpect(model().attribute("error", "비밀번호 재설정 코드가 만료되었습니다."));
    }

}