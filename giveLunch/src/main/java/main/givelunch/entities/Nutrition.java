package main.givelunch.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;

@Entity
@Table(name="nutritions")
@Getter
public class Nutrition {
    @Id
    @Column(name = "food_id")
    private Long foodId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_id")
    private Food food;

    @Column(precision = 8, scale = 2)
    private BigDecimal calories;

    @Column(precision = 8, scale = 2)
    private BigDecimal carbohydrate;

    @Column(precision = 8, scale = 2)
    private BigDecimal protein;

    @Column(precision = 8, scale = 2)
    private BigDecimal fat;

    public static Nutrition from(Food food,FoodAndNutritionDto foodAndNutritionDto){
        NutritionDto nutritionDto = foodAndNutritionDto.getNutrition();
        Nutrition nutrition = new Nutrition();
        nutrition.food = food;
        nutrition.calories = nutritionDto.getCalories();
        nutrition.carbohydrate = nutritionDto.getCarbohydrate();
        nutrition.protein = nutritionDto.getProtein();
        nutrition.fat = nutritionDto.getFat();
        return nutrition;
    }

    public void updateNutrition(FoodAndNutritionDto foodAndNutritionDto){
        NutritionDto nutritionDto = foodAndNutritionDto.getNutrition();
        this.calories = nutritionDto.getCalories();
        this.carbohydrate = nutritionDto.getCarbohydrate();
        this.protein = nutritionDto.getProtein();
        this.fat = nutritionDto.getFat();
    }
}
