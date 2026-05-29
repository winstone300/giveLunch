package main.givelunch.dto.admin;

public record AdminFoodImportResult(
        Integer rowNumber,
        String name,
        AdminFoodImportStatus status,
        Long foodId,
        String reason
) {
    public static AdminFoodImportResult saved(Integer rowNumber, String name, Long foodId) {
        return new AdminFoodImportResult(rowNumber, name, AdminFoodImportStatus.SAVED, foodId, null);
    }

    public static AdminFoodImportResult skipped(Integer rowNumber, String name, Long foodId, String reason) {
        return new AdminFoodImportResult(rowNumber, name, AdminFoodImportStatus.SKIPPED, foodId, reason);
    }

    public static AdminFoodImportResult failed(Integer rowNumber, String name, String reason) {
        return new AdminFoodImportResult(rowNumber, name, AdminFoodImportStatus.FAILED, null, reason);
    }
}
