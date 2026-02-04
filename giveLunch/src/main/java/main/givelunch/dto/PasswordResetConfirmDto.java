package main.givelunch.dto;

public record PasswordResetConfirmDto(
        String email,
        String code,
        String password,
        String passwordConfirm
) {}