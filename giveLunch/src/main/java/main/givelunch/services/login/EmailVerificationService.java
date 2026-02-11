package main.givelunch.services.login;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import main.givelunch.entities.EmailVerification;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.properties.SecurityProperties;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final SecurityProperties securityProperties;
    private static final String ATTEMPT_EXCEEDED_MESSAGE = "시도횟수를 초과했습니다.";
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String mailUsername;

    @Transactional
    public void sendVerificationCode(String email) {
        validateEmail(email);
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException(ErrorCode.DUPLICATE_EMAIL);
        }

        emailVerificationRepository.deleteByEmail(email);

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .verified(false)
                .expiresAt(now.plusMinutes(securityProperties.login().lockMinutes()))
                .createdAt(now)
                .attemptCount(0)
                .build();

        emailVerificationRepository.save(verification);
        sendMail(email, code);
    }

    @Transactional
    public void confirmVerification(String email, String code) {
        validateEmail(email);
        if (code == null || code.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }

        EmailVerification latest = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ValidationException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE));

        LocalDateTime now = LocalDateTime.now();
        if (latest.isBlocked(now)) {
            throw new ValidationException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE, ATTEMPT_EXCEEDED_MESSAGE);
        }
        if (latest.isVerified() || latest.getExpiresAt().isBefore(now)) {
            throw new ValidationException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }

        if (!latest.getCode().equals(code)) {
            throw new ValidationException(
                    ErrorCode.INVALID_EMAIL_VERIFICATION_CODE,
                    increaseAttemptOrBlockAndGetMessage(latest, now));
        }

        latest.setVerified(true);
        latest.setAttemptCount(0);
        latest.setBlockedUntil(null);
    }

    private String increaseAttemptOrBlockAndGetMessage(EmailVerification verification, LocalDateTime now) {
        int nextAttempt = verification.getAttemptCount() + 1;
        if (nextAttempt >= securityProperties.login().maxFailedAttempts()) {
            verification.setBlockedUntil(now.plusMinutes(securityProperties.login().lockMinutes()));
            verification.setAttemptCount(0);
            return ATTEMPT_EXCEEDED_MESSAGE;
        }

        verification.setAttemptCount(nextAttempt);
        int remainingAttempts = securityProperties.login().maxFailedAttempts() - nextAttempt;
        return "인증에 실패했습니다. 남은 시도 횟수: " + remainingAttempts + "회";
    }

    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(email);
        message.setSubject("GiveLunch 회원가입 이메일 인증번호");
        message.setText("회원가입을 위한 이메일 인증번호는 [" + code + "] 입니다. "
                + securityProperties.login().lockMinutes() + "분 내에 입력해주세요.");
        try {
            mailSender.send(message);
        } catch (MailException e) {
            logger.warn("Failed to send verification email to {}", email, e);
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