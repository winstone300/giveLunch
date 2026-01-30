package main.givelunch.validators;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import main.givelunch.dto.SignupRequestDto;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.repositories.EmailVerificationRepository;
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

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

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
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getErrorCode()).isEqualTo(ErrorCode.INVALID_USERNAME);
                })
                .hasMessage(ErrorCode.INVALID_USERNAME.getMessage());
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
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD);
                })
                .hasMessage(ErrorCode.INVALID_PASSWORD.getMessage());
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
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_MISMATCH);
                })
                .hasMessage(ErrorCode.PASSWORD_MISMATCH.getMessage());
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
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME);
                })
                .hasMessage(ErrorCode.DUPLICATE_USERNAME.getMessage());
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
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL);
                })
                .hasMessage(ErrorCode.DUPLICATE_EMAIL.getMessage());
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
        when(emailVerificationRepository.existsByEmailAndVerifiedTrueAndExpiresAtAfter(
                eq("user@example.com"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        assertThatCode(() -> signupValidator.validate(dto)).doesNotThrowAnyException();
    }
    @Test
    @DisplayName("validate() - 이메일이 비어있으면 예외")
    void validate_throwsWhenEmailMissing() {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setUserName("user");
        dto.setPassword("password");
        dto.setPasswordConfirm("password");
        dto.setEmail(" ");

        assertThatThrownBy(() -> signupValidator.validate(dto))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getErrorCode()).isEqualTo(ErrorCode.INVALID_EMAIL);
                })
                .hasMessage(ErrorCode.INVALID_EMAIL.getMessage());
    }

    @Test
    @DisplayName("validate() - 이메일 인증이 안되면 예외")
    void validate_throwsWhenEmailNotVerified() {
        SignupRequestDto dto = new SignupRequestDto();
        dto.setUserName("user");
        dto.setPassword("password");
        dto.setPasswordConfirm("password");
        dto.setEmail("user@example.com");

        when(userRepository.existsByUserName("user")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(emailVerificationRepository.existsByEmailAndVerifiedTrueAndExpiresAtAfter(
                eq("user@example.com"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);

        assertThatThrownBy(() -> signupValidator.validate(dto))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> {
                    ValidationException validationException = (ValidationException) exception;
                    assertThat(validationException.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
                })
                .hasMessage(ErrorCode.EMAIL_NOT_VERIFIED.getMessage());
    }
}