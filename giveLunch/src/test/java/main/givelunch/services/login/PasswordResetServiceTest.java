package main.givelunch.services.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import main.givelunch.entities.PasswordResetToken;
import main.givelunch.entities.UserInfo;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.properties.SecurityProperties;
import main.givelunch.repositories.PasswordResetTokenRepository;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties(
                List.of(),
                List.of("/admin/**"),
                new SecurityProperties.LoginProperties(5, 15,6)
        );
        passwordResetService = new PasswordResetService(
                passwordResetTokenRepository,
                userRepository,
                passwordEncoder,
                mailSender,
                securityProperties,
                new VerificationSupportService());
    }

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
        assertThat(savedToken.isVerified()).isFalse();

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
    @DisplayName("verifyResetCode - 유효한 코드면 verified=true")
    void verifyResetCode_marksTokenVerified() {
        String email = "member@example.com";
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();
        when(passwordResetTokenRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(Optional.of(token));

        passwordResetService.verifyResetCode(email, "123456");

        assertThat(token.isVerified()).isTrue();
    }

    @Test
    @DisplayName("resetPassword - 인증 완료된 유효한 코드면 비밀번호 변경 후 토큰 삭제")
    void resetPassword_updatesPasswordAndMarksTokenUsed() {
        // given
        String email = "member@example.com";
        String code = "123456";
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code(code)
                .verified(true)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        UserInfo user = UserInfo.builder()
                .userName("member")
                .password("oldPassword")
                .email(email)
                .build();

        when(passwordResetTokenRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(Optional.of(token));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");

        // when
        passwordResetService.resetPassword(email, code, "newPassword", "newPassword");

        // then
        assertThat(user.getPassword()).isEqualTo("encodedPassword");
        verify(passwordResetTokenRepository).deleteByEmail(email);
    }

    @Test
    @DisplayName("resetPassword - 코드 인증 안했으면 PASSWORD_RESET_NOT_VERIFIED 예외")
    void resetPassword_throwsWhenNotVerified() {
        String email = "member@example.com";
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code("123456")
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();

        when(passwordResetTokenRepository.findTopByEmailOrderByCreatedAtDesc(email)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword(email, "123456", "newPassword", "newPassword"))                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PASSWORD_RESET_NOT_VERIFIED);
    }
}