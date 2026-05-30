package main.givelunch.dto.admin;

import java.util.List;

public record AdminFoodImportPreviewResponse(
        List<AdminFoodImportPreviewRow> rows,
        long validCount,
        long invalidCount
) {
    public static AdminFoodImportPreviewResponse from(List<AdminFoodImportPreviewRow> rows) {
        long validCount = rows.stream()
                .filter(AdminFoodImportPreviewRow::valid)
                .count();
        long invalidCount = rows.size() - validCount;
        return new AdminFoodImportPreviewResponse(rows, validCount, invalidCount);
    }
}
