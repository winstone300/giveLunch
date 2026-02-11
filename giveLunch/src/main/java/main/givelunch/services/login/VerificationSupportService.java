package main.givelunch.services.login;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import main.givelunch.entities.VerificationCode;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.properties.SecurityProperties;
import org.springdoc.core.service.SecurityService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerificationSupportService {
    private static final String ATTEMPT_EXCEEDED_MESSAGE = "시도횟수를 초과했습니다.";

    private final SecureRandom random = new SecureRandom();
    private final SecurityProperties securityProperties;

    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_EMAIL);
        }
    }

    public void validateCode(String code, ErrorCode errorCode) {
        if (code == null || code.isBlank()) {
            throw new ValidationException(errorCode);
        }
    }

    public String generateCode() {
        StringBuilder sb = new StringBuilder(securityProperties.login().codeLength());
        for (int i = 0; i < securityProperties.login().codeLength(); i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public void verifyCodeAndMarkVerified(
            VerificationCode token,
            String inputCode,
            LocalDateTime now,
            ErrorCode invalidCodeError,
            ErrorCode expiredCodeError,
            boolean rejectWhenAlreadyVerified
    ) {
        if (token.isBlocked(now)) {
            throw new ValidationException(invalidCodeError, ATTEMPT_EXCEEDED_MESSAGE);
        }
        if (rejectWhenAlreadyVerified && token.isVerified()) {
            throw new ValidationException(invalidCodeError);
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new ValidationException(expiredCodeError);
        }
        if (!token.getCode().equals(inputCode)) {
            throw new ValidationException(
                    invalidCodeError,
                    increaseAttemptOrBlock(token, now));
        }

        token.setVerified(true);
        token.setAttemptCount(0);
        token.setBlockedUntil(null);
    }

    public String increaseAttemptOrBlock(VerificationCode token, LocalDateTime now) {
        int nextAttempt = token.getAttemptCount() + 1;
        if (nextAttempt >= securityProperties.login().maxFailedAttempts()) {
            token.setBlockedUntil(now.plusMinutes(securityProperties.login().lockMinutes()));
            token.setAttemptCount(0);
            return ATTEMPT_EXCEEDED_MESSAGE;
        }
        token.setAttemptCount(nextAttempt);

        int remainingAttempts = securityProperties.login().maxFailedAttempts() - nextAttempt;
        return "인증에 실패했습니다. 남은 시도 횟수: " + remainingAttempts + "회";
    }
}