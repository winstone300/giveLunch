package main.givelunch.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;

public record AgentFoodSaveRequest(
        @Schema(description = "DB에 저장할 음식/영양정보 목록")
        @NotEmpty(message = "items는 비어 있을 수 없습니다.")
        List<@Valid FoodAndNutritionDto> items
) {
}
