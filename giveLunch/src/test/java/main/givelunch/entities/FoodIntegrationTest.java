package main.givelunch.entities;

import jakarta.persistence.EntityManager;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.repositories.FoodRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class FoodRepositoryTest {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private EntityManager em;

    // Food 생성 메서드
    private Food createFoodWithNutrition(String name, double calories) {
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setName(name);
        dto.setCategory("테스트 카테고리");
        dto.setServingSizeG(500);

        NutritionDto nutritionDto = new NutritionDto();
        nutritionDto.setCalories(BigDecimal.valueOf(calories));
        dto.setNutrition(nutritionDto);

        return Food.from(dto);
    }

    @Test
    @DisplayName("Food 저장 시 Nutrition도 Cascade 설정을 통해 함께 저장")
    void cascadeSave() {
        // given
        Food food = createFoodWithNutrition("치킨", 2000.0);

        // when
        Food savedFood = foodRepository.save(food);

        // then
        assertThat(savedFood.getId()).isNotNull();
        assertThat(savedFood.getNutrition().getFoodId()).isNotNull();
    }

    @Test
    @DisplayName("이름 중복 금지(Unique) 제약조건이 DB 레벨에서 동작")
    void uniqueNameConstraint() {
        // given
        Food food1 = createFoodWithNutrition("중복음식", 100.0);
        foodRepository.save(food1);

        em.flush();
        em.clear();

        // when & then
        Food food2 = createFoodWithNutrition("중복음식", 200.0); // 같은 이름

        assertThatThrownBy(() -> {
            foodRepository.save(food2);
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("OrphanRemoval 동작 확인: Nutrition 연결을 끊으면 DB에서 삭제된다")
    void orphanRemoval() {
        // given
        Food food = createFoodWithNutrition("피자", 1500.0);
        Food savedFood = foodRepository.save(food);

        em.flush();
        em.clear();

        // when
        Food foundFood = foodRepository.findById(savedFood.getId()).orElseThrow();

        // 연결 끊기
        foundFood.setNutrition(null);

        em.flush();
        em.clear();

        // then
        Food reloadedFood = foodRepository.findById(savedFood.getId()).orElseThrow();

        assertThat(reloadedFood.getNutrition()).isNull();
    }
}