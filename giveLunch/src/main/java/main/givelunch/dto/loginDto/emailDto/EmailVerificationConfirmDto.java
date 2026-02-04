package main.givelunch.dto.loginDto.emailDto;

public record EmailVerificationConfirmDto(
        String email,
        String code
) {
}
