package main.givelunch.controllers;

import lombok.RequiredArgsConstructor;
import main.givelunch.dto.PasswordResetConfirmDto;
import main.givelunch.dto.PasswordResetRequestDto;
import main.givelunch.exception.ValidationException;
import main.givelunch.services.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @Operation(summary = "비밀번호 재설정 요청 화면", description = "이메일로 비밀번호 재설정 코드를 요청하는 화면을 반환")
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "login/forgotPassword";
    }

    @Operation(summary = "비밀번호 재설정 코드 발송", description = "입력한 이메일로 비밀번호 재설정 코드를 전송")
    @PostMapping("/forgot-password")
    public String sendResetCode(@ModelAttribute PasswordResetRequestDto req, Model model) {
        try {
            passwordResetService.sendResetCode(req.email());
            model.addAttribute("message", "비밀번호 재설정 코드가 이메일로 전송되었습니다.");
            return "login/forgotPassword";
        } catch (ValidationException e) {
            model.addAttribute("error", e.getMessage());
            return "login/forgotPassword";
        }
    }

    @Operation(summary = "비밀번호 재설정 화면", description = "재설정 코드와 새 비밀번호를 입력하는 화면을 반환합니다.")
    @GetMapping("/reset-password")
    public String resetPassword() {
        return "login/resetPassword";
    }

    @Operation(summary = "비밀번호 재설정 처리", description = "재설정 코드를 검증하고 새 비밀번호로 변경합니다.")
    @PostMapping("/reset-password")
    public String resetPassword(@ModelAttribute PasswordResetConfirmDto req, Model model) {
        try {
            passwordResetService.resetPassword(req.email(), req.code(), req.password(), req.passwordConfirm());
            return "redirect:/login?resetSuccess";
        } catch (ValidationException e) {
            model.addAttribute("error", e.getMessage());
            return "login/resetPassword";
        }
    }
}