package main.givelunch.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AgentExternalFoodSearchRequest(
        @Schema(description = "검색할 음식명", example = "비빔밥")
        @NotBlank(message = "name은 필수입니다.")
        String name,

        @Schema(description = "외부 API에서 조회할 최대 결과 수. 비워두면 관리자 기본값을 사용합니다.", example = "1")
        @Positive(message = "limit는 1 이상이어야 합니다.")
        Integer limit
) {
}
