package main.givelunch.dto.admin;

import java.util.List;

public record AdminFoodImportResponse(
        List<AdminFoodImportResult> results,
        long savedCount,
        long skippedCount,
        long failedCount
) {
    public static AdminFoodImportResponse from(List<AdminFoodImportResult> results) {
        long savedCount = results.stream()
                .filter(result -> result.status() == AdminFoodImportStatus.SAVED)
                .count();
        long skippedCount = results.stream()
                .filter(result -> result.status() == AdminFoodImportStatus.SKIPPED)
                .count();
        long failedCount = results.stream()
                .filter(result -> result.status() == AdminFoodImportStatus.FAILED)
                .count();
        return new AdminFoodImportResponse(results, savedCount, skippedCount, failedCount);
    }
}
