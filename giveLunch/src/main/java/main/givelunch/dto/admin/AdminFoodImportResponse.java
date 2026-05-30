package main.givelunch.dto.admin;

import java.util.List;

public record AdminFoodImportResponse(
        List<AdminFoodImportResult> results,
        long savedCount,
        long updatedCount,
        long skippedCount,
        long failedCount
) {
    public static AdminFoodImportResponse from(List<AdminFoodImportResult> results) {
        long savedCount = results.stream()
                .filter(result -> result.status() == AdminFoodImportStatus.SAVED)
                .count();
        long updatedCount = results.stream()
                .filter(result -> result.status() == AdminFoodImportStatus.UPDATED)
                .count();
        long skippedCount = results.stream()
                .filter(result -> result.status() == AdminFoodImportStatus.SKIPPED)
                .count();
        long failedCount = results.stream()
                .filter(result -> result.status() == AdminFoodImportStatus.FAILED)
                .count();
        return new AdminFoodImportResponse(results, savedCount, updatedCount, skippedCount, failedCount);
    }
}
