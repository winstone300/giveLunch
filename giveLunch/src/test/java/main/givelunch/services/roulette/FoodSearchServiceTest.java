package main.givelunch.services.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import main.givelunch.dto.FoodAndNutritionDto.FoodDto;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.dto.FoodAndNutritionDto.FoodSuggestionDto;
import main.givelunch.entities.Food;
import main.givelunch.exception.ValidationException;
import main.givelunch.properties.DataGoKrProperties;
import main.givelunch.properties.MenuProperties;
import main.givelunch.repositories.FoodRepository;
import main.givelunch.services.external.DataGoKrFoodClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class FoodSearchServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private DataGoKrFoodClient dataGoKrFoodClient;

    private final DataGoKrProperties dataGoKrProperties = new DataGoKrProperties(
            "https://api.example.com",
            "service-key",
            "/foods",
            "json",
            1,
            8,
            3
    );
    private final MenuProperties menuProperties = new MenuProperties(
            List.of("기본 메뉴"),
            new MenuProperties.SuggestProperties(200, 10)
    );

    private FoodSearchService foodSearchService;

    @BeforeEach
    void setUp() {
        foodSearchService = new FoodSearchService(
                foodRepository,
                dataGoKrFoodClient,
                dataGoKrProperties,
                menuProperties
        );
    }

    @Test
    @DisplayName("getIdByName() - null 입력이면 ValidationException 발생(Repository 호출 없음)")
    void getIdByName_throwsValidationException_whenNameIsNull() {
        assertThatThrownBy(() -> foodSearchService.getIdByName(null))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(foodRepository);
    }

    @Test
    @DisplayName("getIdByName() - 공백만 입력이면 ValidationException 발생(Repository 호출 없음)")
    void getIdByName_throwsValidationException_whenNameIsBlank() {
        assertThatThrownBy(() -> foodSearchService.getIdByName("   "))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(foodRepository);
    }

    @Test
    @DisplayName("getIdByName() - 유효한 이름이면 첫 번째 id 반환")
    void getIdByName_returnsFirstId_whenValidAndResultExists() {
        // given
        String input = "  샐러드  ";
        String normalized = "샐러드";

        when(foodRepository.findIdByName(eq(normalized)))
                .thenReturn(Optional.of(10L));

        // when
        Long result = foodSearchService.getIdByName(input);

        // then
        assertThat(result).isEqualTo(10L);
        verify(foodRepository).findIdByName(eq(normalized));
        verifyNoMoreInteractions(foodRepository);
    }

    @Test
    @DisplayName("getIdByName() - 결과가 없으면 null 반환")
    void getIdByName_returnsNull_whenValidButNoResult() {
        // given
        String input = "김밥";
        String normalized = "김밥";

        when(foodRepository.findIdByName(eq(normalized)))
                .thenReturn(Optional.empty());

        // when
        Long result = foodSearchService.getIdByName(input);

        // then
        assertThat(result).isNull();
        verify(foodRepository).findIdByName(eq(normalized));
        verifyNoMoreInteractions(foodRepository);
    }

    @Test
    @DisplayName("searchExternalFoods: 일반 사용자는 numOfRowsUser 만큼 호출")
    void searchExternalFoods_usesUserRowCount() {
        // given
        UserDetails user = User.withUsername("user").password("pw").roles("USER").build();
        when(dataGoKrFoodClient.fetchFoodsByName("우동", 3)).thenReturn(List.of());

        // when
        List<FoodAndNutritionDto> result = foodSearchService.searchExternalFoods("우동", user);

        // then
        assertThat(result).isEmpty();
        verify(dataGoKrFoodClient).fetchFoodsByName("우동", 3);
        verifyNoMoreInteractions(dataGoKrFoodClient);
    }

    @Test
    @DisplayName("searchExternalFoods: 관리자 사용자는 numOfRowsAdmin 만큼 호출")
    void searchExternalFoods_usesAdminRowCount() {
        // given
        UserDetails admin = User.withUsername("admin").password("pw").roles("ADMIN").build();
        when(dataGoKrFoodClient.fetchFoodsByName("라면", 8)).thenReturn(List.of());

        // when
        List<FoodAndNutritionDto> result = foodSearchService.searchExternalFoods("라면", admin);

        // then
        assertThat(result).isEmpty();
        verify(dataGoKrFoodClient).fetchFoodsByName("라면", 8);
        verifyNoMoreInteractions(dataGoKrFoodClient);
    }

    @Test
    @DisplayName("suggestFoods: 공백 입력이면 빈 리스트 반환(Repository 호출 없음)")
    void suggestFoods_returnsEmpty_whenNameIsBlank() {
        List<FoodSuggestionDto> result = foodSearchService.suggestFoods("   ");

        assertThat(result).isEmpty();
        verifyNoInteractions(foodRepository);
    }

    @Test
    @DisplayName("suggestFoods: prefix 후보를 조회해 앱에서 길이순/이름순 정렬 후 제한 개수만 반환")
    void suggestFoods_sortsAndLimitsInApplication() {
        Food kimchiRice = Food.from(new FoodDto(1L, "김치볶음밥", "한식", null, 100));
        Food kimbap = Food.from(new FoodDto(2L, "김밥", "분식", null, 100));
        Food kimchi = Food.from(new FoodDto(3L, "김치", "한식", null, 100));

        MenuProperties tunedProperties = new MenuProperties(
                List.of("기본 메뉴"),
                new MenuProperties.SuggestProperties(3, 2)
        );
        FoodSearchService tunedService = new FoodSearchService(
                foodRepository,
                dataGoKrFoodClient,
                dataGoKrProperties,
                tunedProperties
        );

        when(foodRepository.findByNameStartingWith("김", PageRequest.of(0, 3)))
                .thenReturn(List.of(kimchiRice, kimchi, kimbap));

        List<FoodSuggestionDto> result = tunedService.suggestFoods("김");

        assertThat(result).extracting(FoodSuggestionDto::name)
                .containsExactly("김밥", "김치");
        verify(foodRepository).findByNameStartingWith("김", PageRequest.of(0, 3));
        verifyNoMoreInteractions(foodRepository);
    }
}
