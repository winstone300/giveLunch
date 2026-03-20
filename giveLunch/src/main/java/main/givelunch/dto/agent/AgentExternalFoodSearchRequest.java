package main.givelunch.dto.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AgentExternalFoodSearchRequest(
        @NotBlank(message = "name은 필수입니다.")
        String name,

        @Positive(message = "limit는 1 이상이어야 합니다.")
        Integer limit
) {
}
