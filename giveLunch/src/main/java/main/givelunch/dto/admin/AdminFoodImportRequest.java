package main.givelunch.dto.admin;

import java.util.List;

public record AdminFoodImportRequest(
        List<AdminFoodImportItem> items
) {
}
