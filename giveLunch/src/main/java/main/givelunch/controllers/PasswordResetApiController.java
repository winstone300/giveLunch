package main.givelunch.controllers;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.loginDto.emailDto.EmailVerificationConfirmDto;
import main.givelunch.services.login.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PasswordResetApiController {
    private final PasswordResetService passwordResetService;

    @Operation(summary = "비밀번호 재설정 코드 인증", description = "사용자가 입력한 재설정 코드가 유효한지 확인")
    @PostMapping("/reset-password/verify")
    public ResponseEntity<Map<String, String>> verifyResetCode(@RequestBody EmailVerificationConfirmDto req) {
        passwordResetService.verifyResetCode(req.email(), req.code());
        return ResponseEntity.ok(Map.of("message", "코드 인증이 완료되었습니다."));
    }
}