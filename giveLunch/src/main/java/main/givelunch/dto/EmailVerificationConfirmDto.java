package main.givelunch.dto;

public record EmailVerificationConfirmDto(
        String email,
        String code
) {
}
