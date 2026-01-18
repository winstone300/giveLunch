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

@Entity
@Table(name="foods")
@Getter
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
        food.id =  foodDto.getId();
        food.name = foodDto.getName();
        food.category = foodDto.getCategory();
        food.imgUrl = foodDto.getImgUrl();
        food.servingSizeG = foodDto.getServingSizeG();
        return food;
    }

    public static Food from(FoodAndNutritionDto foodAndNutritionDto){
        Food food = new Food();
        food.id = foodAndNutritionDto.getFoodId();
        food.name = foodAndNutritionDto.getName();
        food.category = foodAndNutritionDto.getCategory();
        food.imgUrl = foodAndNutritionDto.getImgUrl();
        food.servingSizeG = foodAndNutritionDto.getServingSizeG();
        return food;
    }

    public void updateFood(FoodAndNutritionDto foodAndNutritionDto){
        this.name = foodAndNutritionDto.getName();
        this.category = foodAndNutritionDto.getCategory();
        this.imgUrl = foodAndNutritionDto.getImgUrl();
        this.servingSizeG = foodAndNutritionDto.getServingSizeG();
    }
}
