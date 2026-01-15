package main.givelunch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor(staticName = "of")
@Setter
@NoArgsConstructor
public class FoodAndNutritionDto {
    private Long foodId;
    private String name;
    private String category;
    private String imgUrl;
    private Integer servingSizeG;
    private NutritionDto nutrition;
    private String source;
}
