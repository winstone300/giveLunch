package main.givelunch.validators;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import main.givelunch.dto.SignupRequestDto;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignupValidatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SignupValidator signupValidator;

    @Test
    @DisplayName("validate() - 아이디가 비어있으면 예외")
    void validate_throwsWhenUserNameMissing() {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setUserName(" ");
        dto.setPassword("password");
        dto.setPasswordConfirm("password");
        dto.setEmail("user@example.com");

        assertThatThrownBy(() -> signupValidator.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아이디를 입력해주세요.");
    }

    @Test
    @DisplayName("validate() - 비밀번호가 비어있으면 예외")
    void validate_throwsWhenPasswordMissing() {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setUserName("user");
        dto.setPassword(" ");
        dto.setPasswordConfirm("password");
        dto.setEmail("user@example.com");

        assertThatThrownBy(() -> signupValidator.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호를 입력해주세요.");
    }

    @Test
    @DisplayName("validate() - 비밀번호 확인이 다르면 예외")
    void validate_throwsWhenPasswordMismatch() {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setUserName("user");
        dto.setPassword("password");
        dto.setPasswordConfirm("password2");
        dto.setEmail("user@example.com");

        assertThatThrownBy(() -> signupValidator.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호 확인이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("validate() - 아이디가 이미 있으면 예외")
    void validate_throwsWhenUserNameExists() {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setUserName("user");
        dto.setPassword("password");
        dto.setPasswordConfirm("password");
        dto.setEmail("user@example.com");

        when(userRepository.existsByUserName("user")).thenReturn(true);

        assertThatThrownBy(() -> signupValidator.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");
    }

    @Test
    @DisplayName("validate() - 이메일이 이미 있으면 예외")
    void validate_throwsWhenEmailExists() {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setUserName("user");
        dto.setPassword("password");
        dto.setPasswordConfirm("password");
        dto.setEmail("user@example.com");

        when(userRepository.existsByUserName("user")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> signupValidator.validate(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");
    }

    @Test
    @DisplayName("validate() - 정상 입력이면 예외 없음")
    void validate_passesWhenInputValid() {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setUserName("user");
        dto.setPassword("password");
        dto.setPasswordConfirm("password");
        dto.setEmail("user@example.com");

        when(userRepository.existsByUserName("user")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);

        assertThatCode(() -> signupValidator.validate(dto)).doesNotThrowAnyException();
    }
}