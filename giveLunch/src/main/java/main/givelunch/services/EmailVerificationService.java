package main.givelunch.services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import main.givelunch.entities.EmailVerification;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRE_MINUTES = 10;
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

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .code(code)
                .verified(false)
                .expiresAt(now.plusMinutes(EXPIRE_MINUTES))
                .createdAt(now)
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

        EmailVerification verification = emailVerificationRepository
                .findTopByEmailAndCodeOrderByCreatedAtDesc(email, code)
                .orElseThrow(() -> new ValidationException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE));

        if (verification.isVerified() || verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);
        }

        verification.setVerified(true);
    }

    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailUsername);
        message.setTo(email);
        message.setSubject("GiveLunch 회원가입 이메일 인증번호");
        message.setText("회원가입을 위한 이메일 인증번호는 [" + code + "] 입니다. "
                + EXPIRE_MINUTES + "분 내에 입력해주세요.");
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
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}