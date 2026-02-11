package main.givelunch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetConfirmDto(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식을 입력해주세요.") String email,
        @NotBlank(message = "인증 코드를 입력해주세요.") String code,
        @NotBlank(message = "비밀번호를 입력해주세요.") String password,
        @NotBlank(message = "비밀번호 확인을 입력해주세요.") String passwordConfirm
) {}
