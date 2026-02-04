package main.givelunch.dto.loginDto;

public record SignupRequestDto(
        String userName,
        String password,
        String passwordConfirm,
        String email
) {
}
