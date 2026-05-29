package main.givelunch.services.login;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final VerificationSupportService verificationCodeSupport;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.from-name:GiveLunch}")
    private String mailFromName;

    @Transactional
    public void sendResetCode(String userName, String email) {
        verificationCodeSupport.validateEmail(email);
        if (!userRepository.existsByUserNameAndEmail(userName, email)) {
            throw new ValidationException(ErrorCode.USER_NOT_FOUND);
        }

        passwordResetTokenRepository.deleteByEmail(email);

        String code = verificationCodeSupport.generateCode();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .code(code)
                .verified(false)
                .expiresAt(now.plusMinutes(securityProperties.login().lockMinutes()))
                .createdAt(now)
                .attemptCount(0)
                .build();

        passwordResetTokenRepository.save(token);
        sendMail(email, code);
    }

    @Transactional(noRollbackFor = ValidationException.class)
    public void verifyResetCode(String email, String code) {
        verificationCodeSupport.validateEmail(email);
        verificationCodeSupport.validateCode(code, ErrorCode.INVALID_PASSWORD_RESET_CODE);

        PasswordResetToken token = passwordResetTokenRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new ValidationException(ErrorCode.INVALID_PASSWORD_RESET_CODE));

        verificationCodeSupport.verifyCodeAndMarkVerified(
                token,
                code,
                LocalDateTime.now(),
                ErrorCode.INVALID_PASSWORD_RESET_CODE,
                ErrorCode.PASSWORD_RESET_EXPIRED,
                true);
    }

    @Transactional
    public void resetPassword(String email, String code, String password, String passwordConfirm) {
        verificationCodeSupport.validateEmail(email);
        verificationCodeSupport.validateCode(code, ErrorCode.INVALID_PASSWORD_RESET_CODE);

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
        if (!token.isVerified()) {
            throw new ValidationException(ErrorCode.PASSWORD_RESET_NOT_VERIFIED);
        }
        if (!token.getCode().equals(code)) {
            throw new ValidationException(ErrorCode.INVALID_PASSWORD_RESET_CODE);
        }

        UserInfo userInfo = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        userInfo.setPassword(passwordEncoder.encode(password));
        passwordResetTokenRepository.deleteByEmail(email);
    }



    private void sendMail(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setFrom(createFromAddress());
            helper.setTo(email);
            helper.setSubject("GiveLunch 비밀번호 재설정 코드");
            helper.setText("비밀번호 재설정 코드 [" + code + "] 입니다. "
                    + securityProperties.login().lockMinutes() + "분 내에 입력해주세요.");
            mailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.warn("Failed to send password reset email to {}", email, e);
            throw new ValidationException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private InternetAddress createFromAddress() throws MessagingException, UnsupportedEncodingException {
        if (mailFromName == null || mailFromName.isBlank()) {
            return new InternetAddress(mailFrom);
        }
        return new InternetAddress(mailFrom, mailFromName, StandardCharsets.UTF_8.name());
    }
}
