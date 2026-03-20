package main.givelunch.dto.agent;

import java.util.List;

public record AgentFoodImportResponse(
        List<AgentFoodImportResult> results,
        long savedCount,
        long skippedCount,
        long failedCount
) {
    public static AgentFoodImportResponse from(List<AgentFoodImportResult> results) {
        long savedCount = results.stream()
                .filter(result -> result.status() == AgentFoodImportResultStatus.SAVED)
                .count();
        long skippedCount = results.stream()
                .filter(result -> result.status() == AgentFoodImportResultStatus.SKIPPED)
                .count();
        long failedCount = results.stream()
                .filter(result -> result.status() == AgentFoodImportResultStatus.FAILED)
                .count();
        return new AgentFoodImportResponse(results, savedCount, skippedCount, failedCount);
    }
}
