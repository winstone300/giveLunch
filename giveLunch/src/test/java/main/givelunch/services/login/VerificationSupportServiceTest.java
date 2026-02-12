package main.givelunch.services.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import main.givelunch.entities.PasswordResetToken;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.properties.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VerificationCodeSupport")
class VerificationSupportServiceTest {
    private VerificationSupportService support;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties(
                List.of(),
                List.of("/admin/**"),
                new SecurityProperties.LoginProperties(5, 15,6)
        );
        support = new VerificationSupportService(securityProperties);
    }


    @Test
    @DisplayName("코드가 맞으면 verified=true로 변경하고 시도 정보를 초기화")
    void verifyCodeAndMarkVerified_success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .email("member@example.com")
                .code("123456")
                .verified(false)
                .attemptCount(2)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();

        support.verifyCodeAndMarkVerified(
                token,
                "123456",
                LocalDateTime.now(),
                ErrorCode.INVALID_PASSWORD_RESET_CODE,
                ErrorCode.PASSWORD_RESET_EXPIRED,
                true);

        assertThat(token.isVerified()).isTrue();
        assertThat(token.getAttemptCount()).isZero();
        assertThat(token.getBlockedUntil()).isNull();
    }

    @Test
    @DisplayName("코드 오입력 시 시도 횟수를 증가시킨다")
    void verifyCodeAndMarkVerified_increasesAttemptsOnMismatch() {
        PasswordResetToken token = PasswordResetToken.builder()
                .email("member@example.com")
                .code("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> support.verifyCodeAndMarkVerified(
                token,
                "000000",
                LocalDateTime.now(),
                ErrorCode.INVALID_PASSWORD_RESET_CODE,
                ErrorCode.PASSWORD_RESET_EXPIRED,
                true))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_CODE);

        assertThat(token.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("시도 횟수 초과 시 차단한다")
    void verifyCodeAndMarkVerified_blocksWhenAttemptsExceeded() {
        PasswordResetToken token = PasswordResetToken.builder()
                .email("member@example.com")
                .code("123456")
                .attemptCount(4)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> support.verifyCodeAndMarkVerified(
                token,
                "000000",
                LocalDateTime.now(),
                ErrorCode.INVALID_PASSWORD_RESET_CODE,
                ErrorCode.PASSWORD_RESET_EXPIRED,
                true))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD_RESET_CODE);

        assertThat(token.getAttemptCount()).isZero();
        assertThat(token.getBlockedUntil()).isNotNull();
    }

    @Test
    @DisplayName("block 상태에서 재시도 시 남은 인증 시간을 메시지에 포함")
    void verifyCodeAndMarkVerified_containsRemainingTimeWhenBlocked() {
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = PasswordResetToken.builder()
                .email("member@example.com")
                .code("123456")
                .blockedUntil(now.plusSeconds(90))
                .expiresAt(now.plusMinutes(5))
                .createdAt(now)
                .build();

        assertThatThrownBy(() -> support.verifyCodeAndMarkVerified(
                token,
                "123456",
                now,
                ErrorCode.INVALID_PASSWORD_RESET_CODE,
                ErrorCode.PASSWORD_RESET_EXPIRED,
                true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("남은 인증 시간:");
    }
}