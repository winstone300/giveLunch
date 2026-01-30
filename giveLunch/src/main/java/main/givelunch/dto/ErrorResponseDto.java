package main.givelunch.dto;

public record ErrorResponseDto(
        String code,
        String message) {
}