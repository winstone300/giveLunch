package main.givelunch.services;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.SignupRequest;
import main.givelunch.entities.UserInfo;
import main.givelunch.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {
    private final UserRepository userRepository;

    @Transactional
    public void signup(SignupRequest req){
        String username = req.getUsername();
        String password = req.getPassword();
        String passwordConfirm = req.getPasswordConfirm();
        String email = req.getEmail();

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

        UserInfo user = UserInfo.builder()
                .username(username)
                .password(password)     // 나중에 암호화
                .email(email)
                .build();

        userRepository.save(user);
    }
}
