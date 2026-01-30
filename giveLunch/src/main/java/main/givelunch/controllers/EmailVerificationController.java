package main.givelunch.controllers;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.EmailVerificationConfirmDto;
import main.givelunch.dto.EmailVerificationRequestDto;
import main.givelunch.exception.ValidationException;
import main.givelunch.services.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/signup/email/send")
    public ResponseEntity<Map<String, String>> sendVerification(@RequestBody EmailVerificationRequestDto req) {
        try {
            emailVerificationService.sendVerificationCode(req.email());
            return ResponseEntity.ok(Map.of("message", "인증번호를 이메일로 전송했습니다."));
        } catch (ValidationException e) {
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/signup/email/verify")
    public ResponseEntity<Map<String, String>> verifyCode(@RequestBody EmailVerificationConfirmDto req) {
        try {
            emailVerificationService.confirmVerification(req.email(), req.code());
            return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
        } catch (ValidationException e) {
            return ResponseEntity.status(e.getErrorCode().getStatus())
                    .body(Map.of("message", e.getMessage()));
        }
    }
}