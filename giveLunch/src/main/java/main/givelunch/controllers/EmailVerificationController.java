package main.givelunch.controllers;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.loginDto.emailDto.EmailVerificationConfirmDto;
import main.givelunch.dto.loginDto.emailDto.EmailVerificationRequestDto;
import main.givelunch.exception.ValidationException;
import main.givelunch.services.login.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "인증 메일 전송", description = "입력 받은 이메일 주소로 인증 번호 전송")
    @PostMapping("/signup/email/send")
    public ResponseEntity<Map<String, String>> sendVerification(@RequestBody EmailVerificationRequestDto req) {
        emailVerificationService.sendVerificationCode(req.email());
        return ResponseEntity.ok(Map.of("message", "인증번호를 이메일로 전송했습니다."));
    }

    @Operation(summary = "인증 여부 확인", description = "사용자가 입력한 번호가 인증번호와 일치하는지 확인")
    @PostMapping("/signup/email/verify")
    public ResponseEntity<Map<String, String>> verifyCode(@RequestBody EmailVerificationConfirmDto req) {
        emailVerificationService.confirmVerification(req.email(), req.code());
        return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
    }
}