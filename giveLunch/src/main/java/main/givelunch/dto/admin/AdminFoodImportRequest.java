package main.givelunch.dto.admin;

import java.util.List;

public record AdminFoodImportRequest(
        List<AdminFoodImportItem> items,
        boolean overwriteExisting
) {
    public AdminFoodImportRequest(List<AdminFoodImportItem> items) {
        this(items, false);
    }
}
