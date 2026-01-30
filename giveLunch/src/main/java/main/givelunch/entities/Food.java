package main.givelunch.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.FoodDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.ValidationException;

@Entity
@Table(name="foods")
@Getter
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // name 중복 허용할지?
    @Column(nullable = false, unique = true)
    private String name;
    private String category;

    @Column(name = "img_url",length = 500)
    private String imgUrl;

    @Column(name = "serving_sizeg")
    private Integer servingSizeG;

    @OneToOne(mappedBy = "food", cascade = CascadeType.ALL, orphanRemoval = true)
    private Nutrition nutrition;

    public void setNutrition(Nutrition nutrition) {
        this.nutrition = nutrition;
        if (nutrition != null) {
            nutrition.setFood(this);
        }
    }
    
    public static Food from(FoodDto foodDto){
        Food food = new Food();
        food.id =  foodDto.id();
        food.name = foodDto.name();
        food.category = foodDto.category();
        food.imgUrl = foodDto.imgUrl();
        food.servingSizeG = foodDto.servingSizeG();
        return food;
    }

    public static Food from(FoodAndNutritionDto foodAndNutritionDto){
        Food food = new Food();
        food.id = foodAndNutritionDto.foodId();
        food.name = foodAndNutritionDto.name();
        food.category = foodAndNutritionDto.category();
        food.imgUrl = foodAndNutritionDto.imgUrl();
        food.servingSizeG = foodAndNutritionDto.servingSizeG();
        if(foodAndNutritionDto.nutrition() != null){
            Nutrition nutrition = Nutrition.from(foodAndNutritionDto.nutrition());
            food.setNutrition(nutrition);
        }
        return food;
    }

    public void updateFood(FoodAndNutritionDto dto) {
        validateFoodData(dto);

        this.name = dto.name();
        this.category = dto.category();
        this.imgUrl = dto.imgUrl();
        this.servingSizeG = dto.servingSizeG();

        updateNutritionIfPresent(dto.nutrition());
    }

    private void updateNutritionIfPresent(NutritionDto nutritionDto) {
        if (nutritionDto == null) {
            return; // 비어있으면 nutrition 생성 x
        }

        if (this.nutrition == null) {
            // 기존 nutrition 없으면 새로 생성
            this.setNutrition(Nutrition.from(nutritionDto));
        } else {
            // 기존에 nutrition 있으면 업데이트
            this.nutrition.updateNutrition(nutritionDto);
        }
    }

    private void validateFoodData(FoodAndNutritionDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_FOOD_NAME);
        }
    }
}
