package main.givelunch.dto.agent;

public record AgentFoodImportResult(
        String name,
        AgentFoodImportResultStatus status,
        Long foodId,
        String reason
) {
    public static AgentFoodImportResult saved(String name, Long foodId) {
        return new AgentFoodImportResult(name, AgentFoodImportResultStatus.SAVED, foodId, null);
    }

    public static AgentFoodImportResult skipped(String name, Long foodId, String reason) {
        return new AgentFoodImportResult(name, AgentFoodImportResultStatus.SKIPPED, foodId, reason);
    }

    public static AgentFoodImportResult failed(String name, String reason) {
        return new AgentFoodImportResult(name, AgentFoodImportResultStatus.FAILED, null, reason);
    }
}
