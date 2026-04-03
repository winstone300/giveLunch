package main.givelunch.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AgentFoodImportResponse(
        @Schema(description = "각 요청 항목의 처리 결과 목록")
        List<AgentFoodImportResult> results,
        @Schema(description = "새로 저장된 음식 수", example = "10")
        long savedCount,
        @Schema(description = "이미 존재해서 건너뛴 음식 수", example = "2")
        long skippedCount,
        @Schema(description = "저장 실패한 음식 수", example = "0")
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
