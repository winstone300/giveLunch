package main.givelunch.dto.admin;

import main.givelunch.dto.FoodAndNutritionDto.NutritionDto;

public record AdminFoodImportItem(
        Integer rowNumber,
        String name,
        String category,
        String imgUrl,
        Integer servingSizeG,
        NutritionDto nutrition
) {
}
