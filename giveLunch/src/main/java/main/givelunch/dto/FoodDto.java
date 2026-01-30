package main.givelunch.dto;

import lombok.Builder;
import main.givelunch.entities.Food;

@Builder
public record FoodDto(
        Long id,
        String name,
        String category,
        String imgUrl,
        Integer servingSizeG
) {
    public static FoodDto from(Food food) {
        return FoodDto.builder()
                .id(food.getId())
                .name(food.getName())
                .category(food.getCategory())
                .imgUrl(food.getImgUrl())
                .servingSizeG(food.getServingSizeG())
                .build();
    }
}