package main.givelunch.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import main.givelunch.entities.EmailVerification;
import main.givelunch.entities.PasswordResetToken;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.repositories.PasswordResetTokenRepository;
import org.junit.jupiter.api.AfterEach;
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
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    @AfterEach
    void cleanup() {
        emailVerificationRepository.deleteByEmail("persist-email@example.com");
        passwordResetTokenRepository.deleteByEmail("persist-reset@example.com");
    }

    @Test
    @WithMockUser
    @DisplayName("POST /signup/email/verify: 검증 실패여도 attemptCount가 커밋된다")
    void emailVerifyFailurePersistsAttemptCount() throws Exception {
        emailVerificationRepository.save(EmailVerification.builder()
                .email("persist-email@example.com")
                .code("123456")
                .verified(false)
                .attemptCount(0)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/signup/email/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"persist-email@example.com\",\"code\":\"654321\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL_VERIFICATION_CODE"));

        EmailVerification updated = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc("persist-email@example.com")
                .orElseThrow();
        assertThat(updated.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password/verify: 검증 실패여도 attemptCount가 커밋된다")
    void resetVerifyFailurePersistsAttemptCount() throws Exception {
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .email("persist-reset@example.com")
                .code("123456")
                .verified(false)
                .attemptCount(0)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/reset-password/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"persist-reset@example.com\",\"code\":\"654321\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_RESET_CODE"));

        PasswordResetToken updated = passwordResetTokenRepository
                .findTopByEmailOrderByCreatedAtDesc("persist-reset@example.com")
                .orElseThrow();
        assertThat(updated.getAttemptCount()).isEqualTo(1);
    }
}