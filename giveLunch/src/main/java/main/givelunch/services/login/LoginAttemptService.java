package main.givelunch.services.login;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import main.givelunch.properties.SecurityProperties;
import main.givelunch.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private final UserRepository userRepository;
    private final SecurityProperties securityProperties;

    @Transactional
    public void onLoginSuccess(String userName) {
        userRepository.findByUserName(userName).ifPresent(user -> {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
        });
    }

    @Transactional
    public void onLoginFailure(String userName) {
        userRepository.findByUserName(userName).ifPresent(user -> {
            LocalDateTime now = LocalDateTime.now();
            if (user.isCurrentlyLocked(now)) {
                return;
            }

            int nextCount = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(nextCount);

            int maxFailedAttempts = securityProperties.login().maxFailedAttempts();
            if (nextCount >= maxFailedAttempts) {
                long lockMinutes = securityProperties.login().lockMinutes();
                user.setLockedUntil(now.plusMinutes(lockMinutes));
                user.setFailedLoginCount(0);
            }
        });
    }
}