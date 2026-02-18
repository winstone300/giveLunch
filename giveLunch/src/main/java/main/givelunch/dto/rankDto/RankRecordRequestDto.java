package main.givelunch.dto.rankDto;

import jakarta.validation.constraints.NotBlank;

public record RankRecordRequestDto(
        @NotBlank(message = "음식 이름은 필수입니다.")
        String name) {}
