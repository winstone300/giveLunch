package main.givelunch.dto.rankDto;

import jakarta.validation.constraints.NotNull;

public record RankRecordRequestDto(
        @NotNull
        String name) {}