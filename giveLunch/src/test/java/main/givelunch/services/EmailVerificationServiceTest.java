package main.givelunch.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import main.givelunch.entities.EmailVerification;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.properties.SecurityProperties;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.repositories.UserRepository;
import main.givelunch.services.login.EmailVerificationService;
import main.givelunch.services.login.VerificationSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService")
class EmailVerificationServiceTest {
    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private VerificationSupportService verificationSupportService;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties(
                List.of(),
                List.of("/admin/**"),
                new SecurityProperties.LoginProperties(5, 15,6)
        );
        emailVerificationService = new EmailVerificationService(
                securityProperties,
                emailVerificationRepository,
                userRepository,
                mailSender,
                verificationSupportService
        );
    }


    @Test
    @DisplayName("메일 전송 시 SMTP 계정이 아닌 설정된 발신 주소를 사용")
    void sendVerificationCode_usesConfiguredFromAddress() {
        // given
        String email = "user@example.com";
        String mailFrom = "no-reply@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(verificationSupportService.generateCode()).thenReturn("123456");

        ReflectionTestUtils.setField(emailVerificationService, "mailFrom", mailFrom);

        // when
        emailVerificationService.sendVerificationCode(email);

        // then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isEqualTo(mailFrom);
        assertThat(messageCaptor.getValue().getTo()).containsExactly(email);
        verify(emailVerificationRepository).save(any());
        verify(verificationSupportService).validateEmail(email);
    }

    @Test
    @DisplayName("중복 이메일이면 예외를 던지고 메일 전송/저장을 하지 않음")
    void sendVerificationCode_throwsWhenDuplicateEmail() {
        // given
        String email = "dup@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // when
        assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(email))
                .isInstanceOf(ValidationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

        // then
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(emailVerificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("인증 코드 검증은 공통 지원 로직으로 위임")
    void confirmVerification_delegatesToSharedVerifier() {
        EmailVerification verification = EmailVerification.builder()
                .email("user@example.com")
                .code("111111")
                .verified(false)
                .attemptCount(0)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now())
                .build();
        when(emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc("user@example.com"))
                .thenReturn(Optional.of(verification));

        emailVerificationService.confirmVerification("user@example.com", "222222");

        verify(verificationSupportService).verifyCodeAndMarkVerified(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Boolean.class));
    }
}
