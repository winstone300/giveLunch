package main.givelunch.services.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import main.givelunch.dto.loginDto.SignupRequestDto;
import main.givelunch.entities.UserInfo;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.model.Role;
import main.givelunch.repositories.UserRepository;
import main.givelunch.validators.SignupValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignupService")
class SignupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SignupValidator signupValidator;

    @InjectMocks
    private SignupService signupService;

    @Test
    @DisplayName("signup - 검증 통과 시 USER 권한으로 암호화 저장")
    void signup_savesEncryptedUserWithUserRole() {
        // given
        SignupRequestDto req = new SignupRequestDto("tester", "plain-pass", "plain-pass", "tester@example.com");
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");

        // when
        signupService.signup(req);

        // then
        verify(signupValidator).validate(req);

        ArgumentCaptor<UserInfo> userCaptor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userRepository).save(userCaptor.capture());

        UserInfo saved = userCaptor.getValue();
        assertThat(saved.getUserName()).isEqualTo("tester");
        assertThat(saved.getPassword()).isEqualTo("encoded-pass");
        assertThat(saved.getEmail()).isEqualTo("tester@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("signup - 검증 실패 시 저장하지 않음")
    void signup_doesNotSaveWhenValidationFails() {
        // given
        SignupRequestDto req = new SignupRequestDto("tester", "pass", "pass", "tester@example.com");
        doThrow(new ValidationException(ErrorCode.INVALID_EMAIL)).when(signupValidator).validate(req);

        // when & then
        assertThatThrownBy(() -> signupService.signup(req))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_EMAIL);

        // then
        verify(signupValidator).validate(req);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}