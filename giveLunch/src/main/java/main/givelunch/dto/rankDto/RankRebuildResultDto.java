package main.givelunch.dto.rankDto;

public record RankRebuildResultDto(
        long rebuiltEventCount,
        long rebuiltFoodCount,
        long cutoffEpochSeconds
) {
}
