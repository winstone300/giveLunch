package main.givelunch.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public record AgentFoodImportRequest(
        @Schema(description = "외부 검색 후 저장할 음식 이름 목록", example = "[\"비빔밥\", \"김치찌개\"]")
        @NotEmpty(message = "names는 비어 있을 수 없습니다.")
        List<String> names,

        @Schema(description = "각 음식 이름당 외부 API에서 조회할 최대 결과 수", example = "1")
        @Positive(message = "limitPerName은 1 이상이어야 합니다.")
        Integer limitPerName
) {
}
