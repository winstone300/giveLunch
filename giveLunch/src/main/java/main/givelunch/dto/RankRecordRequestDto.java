package main.givelunch.dto;

import jakarta.validation.constraints.NotNull;

public record RankRecordRequestDto(
        @NotNull
        String name) {}