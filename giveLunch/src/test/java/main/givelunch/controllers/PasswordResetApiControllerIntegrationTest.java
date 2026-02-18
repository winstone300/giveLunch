package main.givelunch.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class PasswordResetApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password/verify: 이메일/코드가 비어있으면 필드 검증 에러 반환")
    void verifyResetCodeReturnsValidationErrorWhenRequestIsBlank() throws Exception {
        mockMvc.perform(post("/reset-password/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"code\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.email").value("이메일을 입력해주세요."))
                .andExpect(jsonPath("$.fields.code").value("인증 코드를 입력해주세요."));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /reset-password/verify: 이메일 형식 오류면 VALIDATION_ERROR 반환")
    void verifyResetCodeReturnsValidationErrorWhenEmailFormatInvalid() throws Exception {
        mockMvc.perform(post("/reset-password/verify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad-email\",\"code\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.email").value("올바른 이메일 형식을 입력해주세요."));
    }
}
