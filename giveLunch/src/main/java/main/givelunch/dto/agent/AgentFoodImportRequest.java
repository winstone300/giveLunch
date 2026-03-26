package main.givelunch.dto.agent;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public record AgentFoodImportRequest(
        @NotEmpty(message = "names는 비어 있을 수 없습니다.")
        List<String> names,

        @Positive(message = "limitPerName은 1 이상이어야 합니다.")
        Integer limitPerName
) {
}
