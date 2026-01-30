package main.givelunch.dto;

import java.math.BigDecimal;

public record NutritionDto(
        BigDecimal calories,
        BigDecimal protein,
        BigDecimal fat,
        BigDecimal carbohydrate
) {
    public static NutritionDto of(BigDecimal calories, BigDecimal protein, BigDecimal fat, BigDecimal carbohydrate) {
        return new NutritionDto(calories, protein, fat, carbohydrate);
    }
}