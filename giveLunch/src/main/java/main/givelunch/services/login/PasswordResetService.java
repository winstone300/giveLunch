package main.givelunch.services.login;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.givelunch.entities.PasswordResetToken;
import main.givelunch.entities.UserInfo;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.properties.SecurityProperties;
import main.givelunch.repositories.PasswordResetTokenRepository;
import main.givelunch.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private static final String ATTEMPT_EXCEEDED_MESSAGE = "시도횟수를 초과했습니다.";

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final SecurityProperties securityProperties;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Transactional
    public void sendResetCode(String userName, String email) {
        if (userName == null || userName.isBlank()){
            throw new ValidationException(ErrorCode.INVALID_USERNAME);
        }
        validateEmail(email);
        if (!userRepository.existsByUserNameAndEmail(userName, email)) {
            throw new ValidationException(ErrorCode.USER_NOT_FOUND);
        }

        passwordResetTokenRepository.deleteByEmail(email);

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code(code)
                .expiresAt(now.plusMinutes(securityProperties.login().lockMinutes()))
                .createdAt(now)
                .attemptCount(0)
                .build();

        passwordResetTokenRepository.save(token);
        sendMail(email, code);
    }

    @Transactional
    public void resetPassword(String email, String code, String password, String passwordConfirm) {
        validateEmail(email);
        if (code == null || code.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_PASSWORD_RESET_CODE);
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_PASSWORD);
        }
        if (!password.equals(passwordConfirm)) {
            throw new ValidationException(ErrorCode.PASSWORD_MISMATCH);
        }

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ValidationException(ErrorCode.INVALID_PASSWORD_RESET_CODE));

        LocalDateTime now = LocalDateTime.now();
        if (token.isBlocked(now)) {
            throw new ValidationException(ErrorCode.INVALID_PASSWORD_RESET_CODE, ATTEMPT_EXCEEDED_MESSAGE);
        }
        if (token.getExpiresAt().isBefore(now)) {
            throw new ValidationException(ErrorCode.PASSWORD_RESET_EXPIRED);
        }
        if (!token.getCode().equals(code)) {
            throw new ValidationException(
                    ErrorCode.INVALID_PASSWORD_RESET_CODE,
                    increaseAttemptOrBlockAndGetMessage(token, now));
        }

        UserInfo userInfo = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        userInfo.setPassword(passwordEncoder.encode(password));
        passwordResetTokenRepository.deleteByEmail(email);
    }

    private String increaseAttemptOrBlockAndGetMessage(PasswordResetToken token, LocalDateTime now) {
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


    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(email);
        message.setSubject("GiveLunch 비밀번호 재설정 코드");
        message.setText("비밀번호 재설정 코드 [" + code + "] 입니다. "
                + securityProperties.login().lockMinutes() + "분 내에 입력해주세요.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send password reset email to {}", email, e);
            throw new ValidationException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_EMAIL);
        }
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(securityProperties.login().codeLength());
        for (int i = 0; i < securityProperties.login().codeLength(); i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}