package main.givelunch.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import main.givelunch.entities.UserInfo;
import main.givelunch.model.Role;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SignupUsernameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /signup/username/check: 사용 가능한 아이디면 available=true 반환")
    void checkUserNameReturnsAvailableWhenUserNameIsNew() throws Exception {
        mockMvc.perform(get("/signup/username/check")
                        .param("userName", "newuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("사용 가능한 아이디입니다."));
    }

    @Test
    @DisplayName("GET /signup/username/check: 이미 가입된 아이디면 available=false 반환")
    void checkUserNameReturnsUnavailableWhenUserNameExists() throws Exception {
        userRepository.save(UserInfo.builder()
                .userName("existing")
                .password("password123")
                .email("existing@example.com")
                .role(Role.USER)
                .build());

        mockMvc.perform(get("/signup/username/check")
                        .param("userName", "existing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."));
    }
}
