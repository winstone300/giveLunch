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

    public static Nutrition from(NutritionDto nutritionDto){
        Nutrition nutrition = new Nutrition();
        nutrition.calories = nutritionDto.calories();
        nutrition.carbohydrate = nutritionDto.carbohydrate();
        nutrition.protein = nutritionDto.protein();
        nutrition.fat = nutritionDto.fat();
        return nutrition;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public void updateNutrition(NutritionDto nutritionDto){
        this.calories = nutritionDto.calories();
        this.carbohydrate = nutritionDto.carbohydrate();
        this.protein = nutritionDto.protein();
        this.fat = nutritionDto.fat();
    }
}
