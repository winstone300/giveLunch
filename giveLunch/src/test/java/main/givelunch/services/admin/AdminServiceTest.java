package main.givelunch.services.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.FoodDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.entities.Food;
import main.givelunch.entities.Nutrition;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.repositories.NutritionRepository;
import main.givelunch.validators.FoodAndNutritionDtoValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private NutritionRepository nutritionRepository;

    @Mock
    private FoodAndNutritionDtoValidator foodAndNutritionDtoValidator;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("loadFoods() - repository 결과를 FoodDto 리스트로 변환")
    void loadFoods_returnsFoodDtos() {
        //given
        FoodDto foodDto1 = FoodDto.builder()
                .id(1L)
                .name("비빔밥")
                .category("외식")
                .imgUrl("img1")
                .servingSizeG(300)
                .build();
        FoodDto foodDto2 = FoodDto.builder()
                .id(2L)
                .name("샐러드")
                .category("가정식")
                .imgUrl("img2")
                .servingSizeG(150)
                .build();

        Food food1 = Food.from(foodDto1);
        Food food2 = Food.from(foodDto2);

        when(foodRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .thenReturn(List.of(food1, food2));

        //when
        List<FoodDto> result = adminService.loadFoods();

        //then
        assertThat(result).hasSize(2);
        assertThat(result.get(0))
                .extracting(FoodDto::getName, FoodDto::getCategory, FoodDto::getImgUrl, FoodDto::getServingSizeG)
                .containsExactly("비빔밥", "외식", "img1", 300);
        assertThat(result.get(1))
                .extracting(FoodDto::getName, FoodDto::getCategory, FoodDto::getImgUrl, FoodDto::getServingSizeG)
                .containsExactly("샐러드", "가정식", "img2", 150);
    }

    @Test
    @DisplayName("deleteFoodsAndNutritions() - 음식과 영양정보 삭제")
    void deleteFoodsAndNutritions_deletesBoth() {
        //given
        Long foodId = 10L;
        Nutrition nutrition = mock(Nutrition.class);

        //when
        adminService.deleteFoodsAndNutritions(foodId);

        //then
        verify(foodRepository).deleteById(foodId);
    }

    @Test
    @DisplayName("saveFoodAndNutrition() - validator 호출 후 저장")
    void saveFood_AndNutrition_savesAfterValidation() {
        //given
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setFoodId(3L);
        dto.setName("파스타");
        dto.setCategory("양식");
        dto.setImgUrl("img");
        dto.setServingSizeG(250);

        ArgumentCaptor<Food> captor = ArgumentCaptor.forClass(Food.class);

        //when
        adminService.saveFoodAndNutrition(dto);

        //then
        verify(foodAndNutritionDtoValidator).hasName(dto);
        verify(foodRepository).save(captor.capture());
        Food saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("파스타");
        assertThat(saved.getCategory()).isEqualTo("양식");
    }

    @Test
    @DisplayName("saveNutrition() - hasNutrition이 null이면 저장하지 않음")
    void saveNutrition_skipsWhenHasNutritionTrue() {
        //given
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setFoodId(3L);
        dto.setName("파스타");
        dto.setCategory("양식");
        dto.setImgUrl("img");
        dto.setServingSizeG(250);

        //when
        adminService.saveFoodAndNutrition(dto);

        //then
        verify(nutritionRepository, never()).save(any(Nutrition.class));
    }

    @Test
    @DisplayName("saveFoodAndNutrition: 영양 정보가 포함된 경우 Food와 함께 Nutrition이 저장") // 이름 수정 권장
    void saveFoodAndNutrition_savesFoodWithNutrition() {
        // given
        NutritionDto nutritionDto = NutritionDto.of(
                new BigDecimal("100"),
                new BigDecimal("10"),
                new BigDecimal("3"),
                new BigDecimal("20"));

        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setName("테스트음식");
        dto.setNutrition(nutritionDto);

        ArgumentCaptor<Food> foodCaptor = ArgumentCaptor.forClass(Food.class);

        // when
        adminService.saveFoodAndNutrition(dto);

        // then
        verify(foodRepository).save(foodCaptor.capture());
        verify(nutritionRepository, never()).save(any());   // food 저장시 자동으로 nutrition도 저장되는 방식

        Food savedFood = foodCaptor.getValue();
        assertThat(savedFood.getNutrition()).isNotNull();
        assertThat(savedFood.getNutrition().getCalories()).isEqualTo(new BigDecimal("100"));
    }

    @Test
    @DisplayName("updateFood() - 음식이 없으면 NOT_FOUND")
    void updateFood_throwsWhenMissing() {
        //given
        Long foodId = 30L;
        FoodAndNutritionDto dto = new FoodAndNutritionDto();

        when(foodRepository.findById(foodId)).thenReturn(Optional.empty());

        //when&then
        assertThatThrownBy(() -> adminService.updateFood(foodId, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException response = (ResponseStatusException) exception;
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(response.getReason()).isEqualTo("음식을 찾을 수 없습니다.");
                });
    }

    @Test
    @DisplayName("updateFood() - 기존 음식 업데이트 후 저장")
    void updateFood_updatesAndSaves() {
        //given
        Long foodId = 40L;
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        Food food = mock(Food.class);

        when(foodRepository.findById(foodId)).thenReturn(Optional.of(food));
        when(foodRepository.save(food)).thenReturn(food);

        //when
        adminService.updateFood(foodId, dto);

        verify(food).updateFood(dto);
        verify(foodRepository).save(food);
    }

    @Test
    @DisplayName("updateNutrition() - 기존 nutrition이 없고 새 nutrition 정보가 있으면 새로 저장")
    void updateNutrition_createsWhenMissingAndHasNutrition() {
        //given
        Long foodId = 50L;
        Food food = mock(Food.class);
        NutritionDto nutritionDto = NutritionDto.of(
                new BigDecimal("200"),
                new BigDecimal("12"),
                new BigDecimal("5"),
                new BigDecimal("30"));
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        dto.setNutrition(nutritionDto);

        when(foodAndNutritionDtoValidator.hasNutrition(dto)).thenReturn(true);
        when(nutritionRepository.findByFoodId(foodId)).thenReturn(Optional.empty());

        //when
        adminService.updateNutrition(foodId, food, dto);

        //then
        verify(nutritionRepository).save(any(Nutrition.class));
    }

    @Test
    @DisplayName("updateNutrition() - 기존 nutrition이 있으면 update 후 저장")
    void updateNutrition_updatesWhenExisting() {
        //given
        Long foodId = 60L;
        Food food = mock(Food.class);
        FoodAndNutritionDto dto = new FoodAndNutritionDto();
        Nutrition nutrition = mock(Nutrition.class);

        when(foodAndNutritionDtoValidator.hasNutrition(dto)).thenReturn(true);
        when(nutritionRepository.findByFoodId(foodId)).thenReturn(Optional.of(nutrition));

        //when
        adminService.updateNutrition(foodId, food, dto);

        //then
        verify(nutrition).updateNutrition(dto);
        verify(nutritionRepository).save(nutrition);
    }

    @Test
    @DisplayName("updateNutrition() - update할 nutrition 정보가 없으면 저장하지 않음")
    void updateNutrition_skipsWhenHasNutritionFalse() {
        Long foodId = 70L;
        Food food = mock(Food.class);
        FoodAndNutritionDto dto = new FoodAndNutritionDto();

        when(foodAndNutritionDtoValidator.hasNutrition(dto)).thenReturn(false);
        when(nutritionRepository.findByFoodId(foodId)).thenReturn(Optional.empty());

        adminService.updateNutrition(foodId, food, dto);

        verify(nutritionRepository, never()).save(any(Nutrition.class));
    }
}