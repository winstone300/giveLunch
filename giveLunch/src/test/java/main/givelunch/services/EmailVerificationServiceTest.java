package main.givelunch.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.repositories.UserRepository;
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

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    @DisplayName("메일 전송 시 발신 주소는 설정된 메일 계정을 사용")
    void sendVerificationCode_usesMailUsernameForFromAddress() {
        // given
        String email = "user@example.com";
        String username = "no-reply@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        ReflectionTestUtils.setField(emailVerificationService, "mailUsername", username);

        // when
        emailVerificationService.sendVerificationCode(email);

        // then
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isEqualTo(username);
        assertThat(messageCaptor.getValue().getTo()).containsExactly(email);
        verify(emailVerificationRepository).save(any());
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
}