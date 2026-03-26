package main.givelunch.dto.agent;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;

public record AgentFoodSaveRequest(
        @NotEmpty(message = "items는 비어 있을 수 없습니다.")
        List<@Valid FoodAndNutritionDto> items
) {
}
