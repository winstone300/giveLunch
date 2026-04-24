package main.givelunch.dto.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record AgentBulkFoodImportRequest(
        @Schema(description = "대량 적재할 음식 이름 목록", example = "[\"비빔밥\", \"김치찌개\"]")
        @NotEmpty(message = "names는 비어 있을 수 없습니다.")
        List<String> names,

        @Schema(description = "각 음식 이름당 외부 API에서 조회할 최대 결과 수, 기본값은 1", example = "1")
        @Positive(message = "limitPerName은 1 이상이어야 합니다.")
        Integer limitPerName
) {
}
