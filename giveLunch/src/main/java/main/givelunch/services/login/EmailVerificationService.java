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
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final VerificationSupportService verificationSupportService;

    @Value("${app.mail.from}")
    private String mailFrom;

    //인증 메일 생성 + 전송 메서드 호출
    @Transactional
    public void sendVerificationCode(String email) {
        verificationSupportService.validateEmail(email);
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException(ErrorCode.DUPLICATE_EMAIL);
        }

        emailVerificationRepository.deleteByEmail(email);

        String code = verificationSupportService.generateCode();
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

    // 인증 확인 & 실패시 시도 횟수 +1
    @Transactional(noRollbackFor = ValidationException.class)
    public void confirmVerification(String email, String code) {
        verificationSupportService.validateEmail(email);
        verificationSupportService.validateCode(code,ErrorCode.INVALID_EMAIL_VERIFICATION_CODE);

        EmailVerification latest = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ValidationException(ErrorCode.INVALID_EMAIL_VERIFICATION_CODE));

        verificationSupportService.verifyCodeAndMarkVerified(
                latest,
                code,
                LocalDateTime.now(),
                ErrorCode.INVALID_EMAIL_VERIFICATION_CODE,
                ErrorCode.INVALID_EMAIL_VERIFICATION_CODE,
                true);
    }

    // 메일 전송
    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
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
}
