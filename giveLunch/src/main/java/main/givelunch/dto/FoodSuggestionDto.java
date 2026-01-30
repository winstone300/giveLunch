package main.givelunch.dto;

import lombok.Builder;
import main.givelunch.entities.Food;

@Builder
public record FoodSuggestionDto(
        Long id,
        String name,
        String imgUrl
) {
    public static FoodSuggestionDto from(Food food) {
        return FoodSuggestionDto.builder()
                .id(food.getId())
                .name(food.getName())
                .imgUrl(food.getImgUrl())
                .build();
    }
}