package main.givelunch.dto;

import java.util.Map;

public record ErrorResponseDto(
        String code,
        String message,
        Map<String, String> fields) {

    public ErrorResponseDto(String code, String message) {
        this(code, message, Map.of());
    }
}