package main.givelunch.dto;

public record SignupRequestDto(
        String userName,
        String password,
        String passwordConfirm,
        String email
) {
}
