package main.givelunch.services.login;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import main.givelunch.entities.VerificationCode;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class VerificationSupportService {
    private static final int CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

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
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
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
            int maxAttempts,
            int blockMinutes,
            boolean rejectWhenAlreadyVerified
    ) {
        if (token.isBlocked(now)) {
            throw new ValidationException(invalidCodeError);
        }
        if (rejectWhenAlreadyVerified && token.isVerified()) {
            throw new ValidationException(invalidCodeError);
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new ValidationException(expiredCodeError);
        }
        if (!token.getCode().equals(inputCode)) {
            increaseAttemptOrBlock(token, now, maxAttempts, blockMinutes);
            throw new ValidationException(invalidCodeError);
        }

        token.setVerified(true);
        token.setAttemptCount(0);
        token.setBlockedUntil(null);
    }

    public void increaseAttemptOrBlock(VerificationCode token, LocalDateTime now, int maxAttempts, int blockMinutes) {
        int nextAttempt = token.getAttemptCount() + 1;
        if (nextAttempt >= maxAttempts) {
            token.setBlockedUntil(now.plusMinutes(blockMinutes));
            token.setAttemptCount(0);
            return;
        }
        token.setAttemptCount(nextAttempt);
    }
}