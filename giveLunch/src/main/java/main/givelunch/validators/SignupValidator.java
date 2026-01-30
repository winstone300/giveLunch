package main.givelunch.validators;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.SignupRequestDto;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.repositories.EmailVerificationRepository;
import main.givelunch.repositories.UserRepository;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SignupValidator {
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    public void validate(SignupRequestDto signupRequestDto) {
        String userName = signupRequestDto.getUserName();
        String password = signupRequestDto.getPassword();
        String passwordConfirm = signupRequestDto.getPasswordConfirm();
        String email = signupRequestDto.getEmail();

        if (userName == null || userName.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_USERNAME);
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_PASSWORD);
        }
        if (email == null || email.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_EMAIL);
        }
        if (!password.equals(passwordConfirm)) {
            throw new ValidationException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (userRepository.existsByUserName(userName)) {
            throw new ValidationException(ErrorCode.DUPLICATE_USERNAME);
        }
        if(userRepository.existsByEmail(email)){
            throw new ValidationException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (!emailVerificationRepository.existsByEmailAndVerifiedTrueAndExpiresAtAfter(email, LocalDateTime.now())) {
            throw new ValidationException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }
}
