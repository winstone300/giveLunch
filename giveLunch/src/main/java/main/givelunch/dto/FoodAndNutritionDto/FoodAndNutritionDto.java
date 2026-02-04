package main.givelunch.dto.FoodAndNutritionDto;

public record FoodAndNutritionDto(
        Long foodId,
        String name,
        String category,
        String imgUrl,
        Integer servingSizeG,
        NutritionDto nutrition,
        String source
) {
    public static FoodAndNutritionDto of(Long foodId, String name, String category,
                                         String imgUrl, Integer servingSizeG, NutritionDto nutrition, String source) {
        return new FoodAndNutritionDto(foodId, name, category, imgUrl, servingSizeG, nutrition, source);
    }
}