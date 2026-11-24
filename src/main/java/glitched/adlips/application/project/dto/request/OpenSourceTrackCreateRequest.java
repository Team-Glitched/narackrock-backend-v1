package glitched.adlips.application.project.dto.request;

public record OpenSourceTrackCreateRequest(
        Long projectId,
        Long userId,
        Long openSourceId,
        int startTick,
        int durationTick
) {}
