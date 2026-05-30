package main.givelunch.dto.admin;

import main.givelunch.dto.FoodAndNutritionDto.NutritionDto;

public record AdminFoodImportPreviewRow(
        int rowNumber,
        String name,
        String category,
        String imgUrl,
        Integer servingSizeG,
        NutritionDto nutrition,
        boolean valid,
        String reason
) {
    public AdminFoodImportItem toItem() {
        return new AdminFoodImportItem(rowNumber, name, category, imgUrl, servingSizeG, nutrition);
    }
}
