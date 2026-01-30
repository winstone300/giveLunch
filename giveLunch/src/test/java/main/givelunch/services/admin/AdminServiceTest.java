package main.givelunch.services.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.FoodDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.exception.ErrorCode;
import main.givelunch.exception.FoodNotFoundException;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private NutritionRepository nutritionRepository;
    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("loadFoods() - repository 결과를 FoodDto 리스트로 변환하여 반환")
    void loadFoods_returnsFoodDtos() {
        // given
        FoodDto dto1 = FoodDto.builder().id(1L).name("비빔밥").category("한식").build();
        FoodDto dto2 = FoodDto.builder().id(2L).name("파스타").category("양식").build();

        when(foodRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .thenReturn(List.of(Food.from(dto1), Food.from(dto2)));

        // when
        List<FoodDto> result = adminService.loadFoods();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("비빔밥");
        assertThat(result.get(1).name()).isEqualTo("파스타");
    }

    @Test
    @DisplayName("deleteFoodsAndNutritions() - ID로 삭제 수행")
    void deleteFoodsAndNutritions_callsDeleteById() {
        // given
        Long foodId = 1L;

        // when
        adminService.deleteFoodsAndNutritions(foodId);

        // then
        // food만 지워도 nutrition이 같이 삭제 됨
        verify(foodRepository).deleteById(foodId);
    }

    @Test
    @DisplayName("saveFoodAndNutrition() - Food와 Nutrition 정보가 함께 저장된다")
    void saveFoodAndNutrition_savesFoodWithNutrition() {
        // given
        NutritionDto nutritionDto = NutritionDto.of(
                new BigDecimal("100"), new BigDecimal("10"),
                new BigDecimal("5"), new BigDecimal("20")
        );

        FoodAndNutritionDto dto = FoodAndNutritionDto.of(
                null,
                "새로운음식",
                "테스트",
                null,
                null,
                nutritionDto,
                null
        );

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);

        // when
        adminService.saveFoodAndNutrition(dto);

        // then
        verify(foodRepository).save(captor.capture());

        Food savedFood = captor.getValue();
        assertThat(savedFood.getName()).isEqualTo("새로운음식");

        assertThat(savedFood.getNutrition()).isNotNull();
        assertThat(savedFood.getNutrition().getCalories()).isEqualTo(new BigDecimal("100"));

        verify(nutritionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateFoodAndNutrition() - 존재하지 않는 음식 ID면 예외 발생")
    void updateFoodAndNutrition_throwsNotFound() {
        // given
        Long invalidId = 999L;
        FoodAndNutritionDto dto = FoodAndNutritionDto.of(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(foodRepository.findById(invalidId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.updateFoodAndNutrition(invalidId, dto))
                .isInstanceOf(FoodNotFoundException.class)
                .satisfies(e -> {
                    FoodNotFoundException foodNotFoundException = (FoodNotFoundException) e;
                    assertThat(foodNotFoundException.getErrorCode()).isEqualTo(ErrorCode.FOOD_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("updateFoodAndNutrition() - 음식과 영양 정보를 업데이트한다")
    void updateFoodAndNutrition_updatesFields() {
        // given
        Long foodId = 1L;

        // 기존 데이터
        FoodDto existingFoodDto = FoodDto.builder().id(foodId).name("구형음식").category("구형").build();
        Food existingFood = Food.from(existingFoodDto);

        // 업데이트할 데이터
        NutritionDto newNutritionDto = NutritionDto.of(
                new BigDecimal("500"), BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE
        );
        FoodAndNutritionDto updateDto = FoodAndNutritionDto.of(
                null,
                "신형음식",
                "신형",
                null,
                null,
                newNutritionDto,
                null
        );

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(existingFood));

        // when
        adminService.updateFoodAndNutrition(foodId, updateDto);

        // then
        assertThat(existingFood.getName()).isEqualTo("신형음식");
        assertThat(existingFood.getNutrition()).isNotNull(); // 없던 영양 정보가 생겼는지
        assertThat(existingFood.getNutrition().getCalories()).isEqualTo(new BigDecimal("500"));

        verify(nutritionRepository, never()).save(any());
    }
}
