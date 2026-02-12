package main.givelunch.services.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import main.givelunch.entities.UserInfo;
import main.givelunch.properties.SecurityProperties;
import main.givelunch.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAttemptService")
class LoginAttemptServiceTest {

    @Mock
    private UserRepository userRepository;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties(
                List.of(),
                List.of("/admin/**"),
                new SecurityProperties.LoginProperties(5, 15,6)
        );
        loginAttemptService = new LoginAttemptService(userRepository, securityProperties);
    }

    @Test
    @DisplayName("로그인 실패 임계치 도달 시 계정 잠금")
    void onLoginFailure_locksUserWhenThresholdReached() {
        UserInfo user = UserInfo.builder().userName("tester").failedLoginCount(4).build();
        when(userRepository.findByUserName("tester")).thenReturn(Optional.of(user));

        boolean locked = loginAttemptService.onLoginFailure("tester");

        assertThat(locked).isTrue();
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getFailedLoginCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("로그인 실패 횟수가 임계치 미만이면 카운트만 증가")
    void onLoginFailure_increasesCountWhenBelowThreshold() {
        UserInfo user = UserInfo.builder().userName("tester").failedLoginCount(1).build();
        when(userRepository.findByUserName("tester")).thenReturn(Optional.of(user));

        boolean locked = loginAttemptService.onLoginFailure("tester");

        assertThat(locked).isFalse();
        assertThat(user.getFailedLoginCount()).isEqualTo(2);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("이미 잠금 상태면 true 반환")
    void onLoginFailure_returnsTrueWhenAlreadyLocked() {
        UserInfo user = UserInfo.builder().userName("tester").failedLoginCount(0).build();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByUserName("tester")).thenReturn(Optional.of(user));

        boolean locked = loginAttemptService.onLoginFailure("tester");

        assertThat(locked).isTrue();
        assertThat(user.getFailedLoginCount()).isZero();
    }

    @Test
    @DisplayName("로그인 성공 시 실패 카운트/잠금 초기화")
    void onLoginSuccess_resetsState() {
        UserInfo user = UserInfo.builder().userName("tester").failedLoginCount(3).build();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByUserName("tester")).thenReturn(Optional.of(user));

        loginAttemptService.onLoginSuccess("tester");

        assertThat(user.getFailedLoginCount()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("잠금 상태면 남은 잠금 시간을 초 단위로 반환")
    void getRemainingLockSeconds_returnsRemainingTime() {
        UserInfo user = UserInfo.builder().userName("tester").failedLoginCount(0).build();
        user.setLockedUntil(LocalDateTime.now().plusSeconds(120));
        when(userRepository.findByUserName("tester")).thenReturn(Optional.of(user));

        long remainingSeconds = loginAttemptService.getRemainingLockSeconds("tester");

        assertThat(remainingSeconds).isGreaterThan(0);
    }
}