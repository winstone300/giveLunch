package main.givelunch.services.roulette;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import main.givelunch.repositories.FoodRepository;
import main.givelunch.validators.FoodNameValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FoodSearchServiceTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private FoodNameValidator foodNameValidator;

    @InjectMocks
    private FoodSearchService foodSearchService;

    @Test
    @DisplayName("getIdByName() - validator false면 null 반환(Repository 호출 없음)")
    void getIdByName_returnsNull_whenNameIsNull() {
        // given
        when(foodNameValidator.isValid(null)).thenReturn(false);

        // when
        Long result = foodSearchService.getIdByName(null);

        // then
        assertThat(result).isNull();
        verify(foodNameValidator).isValid(null);
        verifyNoInteractions(foodRepository);
    }

    @Test
    @DisplayName("getIdByName() - 공백만 입력 -> trim -> validator false -> null 반환(Repository 호출 없음)")
    void getIdByName_returnsNull_whenNameIsBlank() {
        // given
        String input = "   ";
        String normalized = "";
        when(foodNameValidator.isValid(normalized)).thenReturn(false);

        // when
        Long result = foodSearchService.getIdByName(input);

        // then
        assertThat(result).isNull();
        verify(foodNameValidator).isValid(normalized);
        verifyNoInteractions(foodRepository);
    }

    @Test
    @DisplayName("getIdByName() - validator가 true -> 첫 번째 id 반환")
    void getIdByName_returnsFirstId_whenValidAndResultExists() {
        // given
        String input = "  샐러드  ";
        String normalized = "샐러드";

        when(foodNameValidator.isValid(normalized)).thenReturn(true);
        when(foodRepository.findIdByNameContaining(eq(normalized), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of(10L));

        // when
        Long result = foodSearchService.getIdByName(input);

        // then
        assertThat(result).isEqualTo(10L);
        verify(foodNameValidator).isValid(normalized);
        verify(foodRepository).findIdByNameContaining(eq(normalized), eq(PageRequest.of(0, 1)));
        verifyNoMoreInteractions(foodRepository);
    }

    @Test
    @DisplayName("getIdByName() - validator가 true지만 결과가 없으면 null 반환")
    void getIdByName_returnsNull_whenValidButNoResult() {
        // given
        String input = "김밥";
        String normalized = "김밥";

        when(foodNameValidator.isValid(normalized)).thenReturn(true);
        when(foodRepository.findIdByNameContaining(eq(normalized), eq(PageRequest.of(0, 1))))
                .thenReturn(List.of());

        // when
        Long result = foodSearchService.getIdByName(input);

        // then
        assertThat(result).isNull();
        verify(foodNameValidator).isValid(normalized);
        verify(foodRepository).findIdByNameContaining(eq(normalized), eq(PageRequest.of(0, 1)));
        verifyNoMoreInteractions(foodRepository);
    }
}