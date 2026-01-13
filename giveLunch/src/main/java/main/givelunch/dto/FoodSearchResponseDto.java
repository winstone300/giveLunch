package main.givelunch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import main.givelunch.entities.Food;

@Getter
@AllArgsConstructor(staticName = "of")
public class FoodSearchResponseDto {
    private Long id;
    private String name;
    private String category;

    public static FoodSearchResponseDto from(Food food) {
        return FoodSearchResponseDto.of(food.getId(), food.getName(), food.getCategory());
    }
}
