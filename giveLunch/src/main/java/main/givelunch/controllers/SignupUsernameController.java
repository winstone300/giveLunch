package main.givelunch.controllers;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;
import main.givelunch.services.login.SignupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SignupUsernameController {
    private final SignupService signupService;

    @GetMapping("/signup/username/check")
    public ResponseEntity<Map<String, Object>> checkUserName(@RequestParam String userName) {
        if (userName == null || userName.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_USERNAME);
        }

        boolean available = signupService.isUserNameAvailable(userName);
        String message = available ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";

        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", message
        ));
    }
}
