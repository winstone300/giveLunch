package main.givelunch.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import main.givelunch.entities.EmailVerification;
import main.givelunch.entities.UserInfo;
import main.givelunch.model.Role;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "management.health.mail.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmailVerificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    @WithMockUser
    @DisplayName("POST /signup/email/send: 인증 코드 발송 후 email_verifications 테이블에 저장")
    void sendVerificationSavesCode() throws Exception {
        mockMvc.perform(post("/signup/email/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newuser@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("인증번호를 이메일로 전송했습니다."))
                .andExpect(jsonPath("$.remainingSeconds").value("600"));

        EmailVerification saved = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc("newuser@example.com")
                .orElseThrow();
        assertThat(saved.getCode()).hasSize(6);
        assertThat(saved.isVerified()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /signup/email/verify: 인증 코드 확인 성공 시 verified=true")
    void verifyCodeMarksAsVerified() throws Exception {
        emailVerificationRepository.save(EmailVerification.builder()
                .email("verify@example.com")
                .code("123456")
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/signup/email/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"verify@example.com\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));

        EmailVerification verified = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc("verify@example.com")
                .orElseThrow();
        assertThat(verified.isVerified()).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("POST /signup/email/verify: 인증 실패 시 남은 시도 횟수 메시지 반환")
    void verifyCodeReturnsRemainingAttemptsMessageOnFailure() throws Exception {
        emailVerificationRepository.save(EmailVerification.builder()
                .email("fail@example.com")
                .code("123456")
                .verified(false)
                .attemptCount(0)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/signup/email/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"fail@example.com\",\"code\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("인증에 실패했습니다. 남은 시도 횟수: 4회"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /signup/email/send: 이미 가입된 이메일이면 409 반환")
    void sendVerificationReturnsConflictForDuplicateEmail() throws Exception {
        userRepository.save(UserInfo.builder()
                .userName("existing")
                .password("encodedPassword")
                .email("existing@example.com")
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/signup/email/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"existing@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }
}