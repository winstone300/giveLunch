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
        if(foodAndNutritionDto.getNutrition() != null){
            Nutrition nutrition = Nutrition.from(foodAndNutritionDto.getNutrition());
            food.setNutrition(nutrition);
        }
        return food;
    }

    public void updateFood(FoodAndNutritionDto dto) {
        validateFoodData(dto);

        this.name = dto.getName();
        this.category = dto.getCategory();
        this.imgUrl = dto.getImgUrl();
        this.servingSizeG = dto.getServingSizeG();

        updateNutritionIfPresent(dto.getNutrition());
    }

    // [분리된 메서드 1]: 영양 정보 처리 로직만 담당
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
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("음식 이름은 필수입니다.");
        }
    }
}
