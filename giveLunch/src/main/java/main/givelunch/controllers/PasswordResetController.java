package main.givelunch.controllers;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import main.givelunch.dto.PasswordResetConfirmDto;
import main.givelunch.dto.PasswordResetRequestDto;
import main.givelunch.dto.loginDto.emailDto.EmailVerificationConfirmDto;
import main.givelunch.services.login.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @Operation(summary = "비밀번호 재설정 요청 화면", description = "이메일로 비밀번호 재설정 코드를 요청하는 화면을 반환")
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @Operation(summary = "비밀번호 재설정 코드 발송", description = "입력한 이메일로 비밀번호 재설정 코드를 전송")
    @PostMapping("/forgot-password")
    public String sendResetCode(@ModelAttribute PasswordResetRequestDto req) {
        passwordResetService.sendResetCode(req.userName(), req.email());
        return "redirect:/reset-password";
    }

    @Operation(summary = "비밀번호 재설정 화면", description = "재설정 코드와 새 비밀번호를 입력하는 화면을 반환")
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @Operation(summary = "비밀번호 재설정 처리", description = "재설정 코드를 검증하고 새 비밀번호로 변경")
    @PostMapping("/reset-password")
    public String resetPassword(@ModelAttribute PasswordResetConfirmDto req) {
        passwordResetService.resetPassword(req.email(), req.code(), req.password(), req.passwordConfirm());
        return "redirect:/login?resetSuccess";
    }

    @Operation(summary = "비밀번호 재설정 코드 인증", description = "사용자가 입력한 재설정 코드가 유효한지 확인")
    @PostMapping("/reset-password/verify")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyResetCode(@RequestBody EmailVerificationConfirmDto req) {
        passwordResetService.verifyResetCode(req.email(), req.code());
        return ResponseEntity.ok(Map.of("message", "코드 인증이 완료되었습니다."));
    }
}