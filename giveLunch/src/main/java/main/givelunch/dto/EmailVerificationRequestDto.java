package main.givelunch.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequestDto(
        //MethodArgumentNotValidException 처리
        @NotBlank @Email String email
) {
}
