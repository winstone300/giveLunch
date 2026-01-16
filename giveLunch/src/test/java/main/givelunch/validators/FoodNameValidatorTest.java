package main.givelunch.validators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FoodNameValidatorTest {

    private final FoodNameValidator foodNameValidator = new FoodNameValidator();

    @Test
    @DisplayName("isValid() - null이면 false 반환")
    void isValid_returnsFalse_whenNull() {
        assertThat(foodNameValidator.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("isValid() - 공백이면 false 반환")
    void isValid_returnsFalse_whenBlank() {
        assertThat(foodNameValidator.isValid("   ")).isFalse();
    }

    @Test
    @DisplayName("isValid() - 공백 + 값이면 true")
    void isValid_returnsFalse_whenBlankAndValue() {
        assertThat(foodNameValidator.isValid("  샐러드 ")).isTrue();
    }

    @Test
    @DisplayName("isValid() - 공백 + 특주문자면 false")
    void isValid_returnsFalse_whenBlankAndSpecialCharacter() {
        assertThat(foodNameValidator.isValid(" \n\t  ")).isFalse();
    }

    @Test
    @DisplayName("isValid() - 값이 있으면 true 반환")
    void isValid_returnsTrue_whenHasValue() {
        assertThat(foodNameValidator.isValid("샐러드")).isTrue();
    }
}