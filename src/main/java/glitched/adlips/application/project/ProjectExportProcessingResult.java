package glitched.adlips.application.project;

public record ProjectExportProcessingResult(
        int discoveredCount,
        int completedCount,
        int failedCount,
        int skippedCount
) {
}
