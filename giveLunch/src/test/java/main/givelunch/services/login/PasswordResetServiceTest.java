package main.givelunch.services.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import main.givelunch.entities.PasswordResetToken;
import main.givelunch.entities.UserInfo;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.repositories.PasswordResetTokenRepository;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService")
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    @DisplayName("sendResetCode - 유저 정보가 맞으면 토큰 저장 후 메일 전송")
    void sendResetCode_savesTokenAndSendsMail() {
        // given
        String username = "tester";
        String email = "tester@example.com";
        ReflectionTestUtils.setField(passwordResetService, "mailUsername", "no-reply@givelunch.com");

        when(userRepository.existsByUserNameAndEmail(username, email)).thenReturn(true);

        // when
        passwordResetService.sendResetCode(username, email);

        // then
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getEmail()).isEqualTo(email);
        assertThat(savedToken.getCode()).matches("\\d{6}");
        assertThat(savedToken.getExpiresAt()).isAfter(savedToken.getCreatedAt());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getFrom()).isEqualTo("no-reply@givelunch.com");
        assertThat(mailCaptor.getValue().getTo()).containsExactly(email);
    }

    @Test
    @DisplayName("sendResetCode - 사용자 없으면 USER_NOT_FOUND 예외")
    void sendResetCode_throwsWhenUserNotFound() {
        // given
        when(userRepository.existsByUserNameAndEmail("missing", "missing@example.com")).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> passwordResetService.sendResetCode("missing", "missing@example.com"))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("resetPassword - 유효한 코드면 비밀번호 변경 후 토큰을 사용 처리")
    void resetPassword_updatesPasswordAndMarksTokenUsed() {
        // given
        String email = "member@example.com";
        String code = "123456";
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code(code)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        UserInfo user = UserInfo.builder()
                .userName("member")
                .password("oldPassword")
                .email(email)
                .build();

        when(passwordResetTokenRepository.findTopByEmailOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(token));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");

        // when
        passwordResetService.resetPassword(email, code, "newPassword", "newPassword");

        // then
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
    }

    @Test
    @DisplayName("resetPassword - 비밀번호 확인 불일치면 PASSWORD_MISMATCH 예외")
    void resetPassword_throwsWhenPasswordMismatch() {
        // when & then
        assertThatThrownBy(() -> passwordResetService.resetPassword("a@a.com", "123456", "pw1", "pw2"))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_MISMATCH);
    }

    @Test
    @DisplayName("resetPassword - 만료된 코드면 PASSWORD_RESET_EXPIRED 예외")
    void resetPassword_throwsWhenCodeExpired() {
        // given
        String email = "member@example.com";
        String code = "123456";
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .email(email)
                .code(code)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(passwordResetTokenRepository.findTopByEmailOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(expiredToken));

        // when & then
        assertThatThrownBy(() -> passwordResetService.resetPassword(email, code, "newPassword", "newPassword"))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_RESET_EXPIRED);
    }

    @Test
    @DisplayName("resetPassword - 코드 오입력 누적 시 차단 처리")
    void resetPassword_blocksAfterTooManyAttempts() {
        String email = "member@example.com";
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code("123456")
                .attemptCount(4)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(passwordResetTokenRepository.findTopByEmailOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(email, "999999", "newPassword", "newPassword"))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_CODE);

        assertThat(token.getBlockedUntil()).isNotNull();
        assertThat(token.getAttemptCount()).isEqualTo(0);
    }
}