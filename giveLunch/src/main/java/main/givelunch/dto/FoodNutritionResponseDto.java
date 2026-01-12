package main.givelunch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class FoodNutritionResponseDto {
    private Long foodId;
    private String name;
    private String category;
    private String imgUrl;
    private Integer servingSizeG;
    private NutritionDto nutrition;
    private String source;
}
