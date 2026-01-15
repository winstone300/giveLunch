package main.givelunch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import main.givelunch.entities.Food;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodDto {
    private Long id;
    private String name;
    private String category;
    private String imgUrl;
    private Integer servingSizeG;

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
