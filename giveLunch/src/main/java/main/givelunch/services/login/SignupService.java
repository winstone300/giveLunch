package main.givelunch.services.login;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.SignupRequestDto;
import main.givelunch.entities.UserInfo;
import main.givelunch.repositories.UserRepository;
import main.givelunch.validators.SignupValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignupService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SignupValidator signupValidator;

    @Transactional
    public void signup(SignupRequestDto req){
        signupValidator.validate(req);

        UserInfo user = UserInfo.builder()
                .userName(req.getUserName())
                .password(passwordEncoder.encode(req.getPassword()))     // 암호화
                .email(req.getEmail())
                .build();

        userRepository.save(user);
    }
}
