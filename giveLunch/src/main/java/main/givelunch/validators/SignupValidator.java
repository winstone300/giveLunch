package main.givelunch.validators;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.SignupRequest;
import main.givelunch.repositories.UserRepository;
import main.givelunch.services.SignupService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignupValidator {
    private final UserRepository userRepository;

    public void validate(SignupRequest signupRequest) {
        String username = signupRequest.getUsername();
        String password = signupRequest.getPassword();
        String passwordConfirm = signupRequest.getPasswordConfirm();
        String email = signupRequest.getEmail();

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }
        if (!password.equals(passwordConfirm)) {
            throw new IllegalArgumentException("비밀번호 확인이 일치하지 않습니다.");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if(userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
    }
}
